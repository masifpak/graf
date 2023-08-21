package fi.free.onefree.grupi.batch.filestager.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import fi.free.onefree.grupi.batch.filestager.item.StgGrupiItemProcessor;
import fi.free.onefree.grupi.batch.filestager.listener.GrupiFileStagerJobExecutionListener;
import fi.free.onefree.grupi.batch.filestager.listener.GrupiFileStagerStepExecutionListener;
import fi.free.onefree.grupi.batch.filestager.mapper.StgGrupiFileLineMapper;
import fi.free.onefree.grupi.batch.filestager.tasklet.GrupiFileDownloadTasklet;
import fi.free.onefree.grupi.entity.StgGrupiItem;
import fi.free.onefree.grupi.repo.StgGrupiItemRepo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableBatchProcessing
public class GrupiFileStagerBatchConfiguration {

	@Value("${onefree.grupi.file-stager.chunk-size:50}")
	private int chunkSize;

	@Value("${onefree.grupi.file-stager.concurrency-limit:10}")
	private int concurrencyLimit;

	@Value("${onefree.grupi.file-stager.throttle-limit:5}")
	private int throttleLimit;

	@Value("${onefree.files.temp.root-folder:/tmp/one-free}")
	private String localTempFolder;

	@Value("${onefree.grupi.file-stager.columns:col_01,col_02,col_03,col_04,col_05,col_06,col_07,col_08,col_09}")
	private String[] columns;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	@StepScope
	public ItemStreamReader<StgGrupiItem> grupiFileItemReader(
			@Value("#{jobExecutionContext['FILE_NAME_WITH_ABS_PATH']}") String grupiFileNameWithAbsPath,
			ResourceLoader resourceLoader) {
		log.trace("In grupiFileItemReader");

		if (!grupiFileNameWithAbsPath.matches("[a-z]+:.*")) {
			grupiFileNameWithAbsPath = "file:" + grupiFileNameWithAbsPath;
		}
		log.info("grupiFileNameWithAbsPath: {}", grupiFileNameWithAbsPath);

		DefaultLineMapper<StgGrupiItem> stgGrupiLineMapper = stgGrupiLineMapper();

		// FlatFileItemReader should be wrapped to SynchronizedItemStreamReader to avoid incorrect row number assignment in concurrent file reading process
		var grupiFileItemReader = new FlatFileItemReaderBuilder<StgGrupiItem>().name("grupiFileItemReader")
				.resource(resourceLoader.getResource(grupiFileNameWithAbsPath))
				.recordSeparatorPolicy(new DefaultRecordSeparatorPolicy())
				.lineMapper(stgGrupiLineMapper)
				.build();

		return new SynchronizedItemStreamReaderBuilder<StgGrupiItem>().delegate(grupiFileItemReader).build();
	}

	@Bean
	@StepScope
	public StgGrupiItemProcessor stgGrupiItemProcessor(
			@Value("#{jobParameters['STG_GRUPI_FILE_ID']}") Long stgGrupiFileId) {
		log.trace("In stgGrupiItemProcessor");

		return new StgGrupiItemProcessor(stgGrupiFileId);
	}

	@Bean
	public RepositoryItemWriter<StgGrupiItem> grupiStgItemWriter(StgGrupiItemRepo stgGrupiItemRepo) {
		log.trace("In grupiStgItemWriter");

		RepositoryItemWriter<StgGrupiItem> writer = new RepositoryItemWriter<>();
		writer.setRepository(stgGrupiItemRepo);
		writer.setMethodName("save");
		return writer;
	}

	@Bean
	public Job grupiFileStagerJob(@Qualifier("grupiFileDownloadStep") Step grupiFileDownloadStep,
			@Qualifier("grupiFileStagerStep") Step grupiFileStagerStep,
			GrupiFileStagerJobExecutionListener grupiFileStagerJobExecutionListener,
			JobBuilderFactory jobBuilderFactory) {
		log.trace("In grupiFileStagerJob");

		return jobBuilderFactory.get("grupiFileStagerJob")
				.incrementer(new GrupiRunIdIncrementer())
				.preventRestart()
				.listener(grupiFileStagerJobExecutionListener)
				.flow(grupiFileDownloadStep)
				.next(grupiFileStagerStep)
				.end()
				.build();
	}

	@Bean
	public Step grupiFileStagerStep(ItemStreamReader<StgGrupiItem> grupiFileItemReader,
			ItemWriter<StgGrupiItem> stgGrupiItemWriter,
			StgGrupiItemProcessor stgGrupiItemProcessor) {

		log.trace("In grupiFileStagerStep");

		return stepBuilderFactory.get("grupiFileStagerStep")
				.<StgGrupiItem, StgGrupiItem>chunk(chunkSize)
				.reader(grupiFileItemReader)
				.processor(stgGrupiItemProcessor)
				.writer(stgGrupiItemWriter)
				.taskExecutor(stgGrupiFileStagerTaskExecutor())
				.throttleLimit(throttleLimit)
				.listener(grupiFileStagerStepExecutionListener())
				.build();
	}

	@Bean
	public TaskletStep grupiFileDownloadStep(GrupiFileDownloadTasklet grupiFileDownloadTasklet) {
		log.trace("In grupiFileDownloadStep");

		return stepBuilderFactory.get("grupiFileDownloadStep").tasklet(grupiFileDownloadTasklet).build();
	}

	@Bean
	public TaskExecutor stgGrupiFileStagerTaskExecutor() {
		var taskExecutor = new SimpleAsyncTaskExecutor("stgGrupiFileStagerStepThread");
		taskExecutor.setConcurrencyLimit(concurrencyLimit);

		return taskExecutor;
	}

	@Bean
	public DefaultLineMapper<StgGrupiItem> stgGrupiLineMapper() {
		log.trace("In stgGrupiLineMapper");

		DefaultLineMapper<StgGrupiItem> lineMapper = new StgGrupiFileLineMapper();
		var delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setNames(columns);
		delimitedLineTokenizer.setStrict(false);
		lineMapper.setLineTokenizer(delimitedLineTokenizer);

		var fieldSetMapper = new BeanWrapperFieldSetMapper<StgGrupiItem>();
		fieldSetMapper.setTargetType(StgGrupiItem.class);

		lineMapper.setFieldSetMapper(fieldSetMapper);
		return lineMapper;
	}

	@Bean
	public GrupiFileStagerStepExecutionListener grupiFileStagerStepExecutionListener() {
		log.trace("In stgGrupiFileStagerStepExecutionListener");

		return new GrupiFileStagerStepExecutionListener();
	}

	@Bean
	@Primary
	public BatchConfigurer batchConfigurer(@Qualifier("dataflowDataSource") DataSource dataSource) {
		return new DefaultBatchConfigurer(dataSource);
	}

	@Bean
	@Primary
	public DefaultTaskConfigurer taskConfigurer(@Qualifier("dataflowDataSource") DataSource dataSource) {
		return new DefaultTaskConfigurer(dataSource);
	}

}