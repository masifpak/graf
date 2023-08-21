package fi.free.onefree.grupi.batch.filestager.tasklet;

import java.nio.file.Paths;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import fi.free.flj.cloud_storage.service.FileStorageService;
import fi.free.onefree.grupi.batch.filestager.GrupiFileStagerConstants;
import fi.free.onefree.grupi.batch.filestager.exception.GrupiBatchException;
import fi.free.onefree.grupi.batch.filestager.util.ParamExtractor;
import fi.free.onefree.grupi.enums.StgGrupiFileStatusEnum;
import fi.free.onefree.grupi.repo.StgGrupiFileRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class GrupiFileDownloadTasklet implements Tasklet {

	private final FileStorageService fileStorageService;
	private final StgGrupiFileRepo stgGrupiFileRepo;

	@Value("${onefree.files.grupi.rel-path:grupi}")
	private String grupiFileDirRelPath;

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		var stepContext = chunkContext.getStepContext();

		var stgGrupiFileId = ParamExtractor.getStgGrupiFileId(stepContext);
		log.info("JobParameter STG_GRUPI_FILE_ID: {}", stgGrupiFileId);

		var stgGrupiFile = stgGrupiFileRepo.findById(stgGrupiFileId)
				.orElseThrow(() -> new GrupiBatchException("Not found StgGrupiFile(" + stgGrupiFileId + ")"));
		log.debug("StgGrupiFile({}).status: {}", stgGrupiFile.getId(), stgGrupiFile.getStatus());

		Assert.state(stgGrupiFile.getStatus() == StgGrupiFileStatusEnum.FILE_PARSE_IN_PROGRESS,
				"Invalid status: " + stgGrupiFile.getStatus() + " for StgGrupiFile(" + stgGrupiFileId + ")");

		var grupiCompanyFolderName = stgGrupiFile.getGrupiCompany().getFolderName();
		log.trace("grupiCompanyFolderName: {}", grupiCompanyFolderName);

		var grupiFileName = stgGrupiFile.getFileNameGenerated();
		log.info("grupiFileName: {}", grupiFileName);

		var grupiFileNameWithPath = Paths.get(grupiFileDirRelPath, grupiCompanyFolderName, grupiFileName).toString();
		log.trace("For StgGrupiFile({}), grupiFileNameWithPath: {}", stgGrupiFileId, grupiFileNameWithPath);

		if (grupiFileNameWithPath != null) {
			var grupiFile = fileStorageService.fetchFile(grupiFileNameWithPath);
			log.debug("grupiFile: {}", grupiFile.getName());

			var jobExecutionContext = stepContext.getStepExecution().getJobExecution().getExecutionContext();
			jobExecutionContext.put(GrupiFileStagerConstants.FILE_NAME_WITH_ABS_PATH, grupiFile.getAbsolutePath());
		} else {
			stepContext.getStepExecution().setStatus(BatchStatus.FAILED);
		}

		return RepeatStatus.FINISHED;
	}

}