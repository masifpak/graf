package fi.free.onefree.grupi.batch.filestager.listener;

import java.util.Objects;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import fi.free.flj.util.StringUtil;
import fi.free.onefree.grupi.batch.filestager.exception.GrupiBatchException;
import fi.free.onefree.grupi.batch.filestager.util.ParamExtractor;
import fi.free.onefree.grupi.enums.StgGrupiFileStatusEnum;
import fi.free.onefree.grupi.repo.StgGrupiFileRepo;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener for Grupi file staging job execution events.
 */
@Slf4j
@Component
public class GrupiFileStagerJobExecutionListener {

	@Autowired
	private StgGrupiFileRepo stgGrupiFileRepo;

	@BeforeJob
	public void beforeJob(JobExecution jobExecution) {
		log.trace("In beforeJob");

		ParamExtractor.validateJobParamStgGrupiFileId(jobExecution.getJobParameters());

		var stgGrupiFileId = ParamExtractor.getStgGrupiFileId(jobExecution);
		log.info("JobParameter STG_GRUPI_FILE_ID:{}", stgGrupiFileId);

		var stgGrupiFile = stgGrupiFileRepo.findById(stgGrupiFileId)
				.orElseThrow(() -> new GrupiBatchException("Not found StgGrupiFile(" + stgGrupiFileId + ")"));
		log.debug("StgGrupiFile({}).status: {}", stgGrupiFile.getId(), stgGrupiFile.getStatus());

		// saving JobExecutionId to description
		stgGrupiFile.setDescription(
				ParamExtractor.getJobExecutionMessage(jobExecution.getId(), stgGrupiFile.getDescription()));
		stgGrupiFile = stgGrupiFileRepo.save(stgGrupiFile);
		log.info("StgGrupiFile({}) description updated with JobExecutionId={}", stgGrupiFile.getId(),
				jobExecution.getId());

		Assert.state(stgGrupiFile.getStatus() == StgGrupiFileStatusEnum.FILE_PARSE_PENDING,
				"Invalid status: " + stgGrupiFile.getStatus() + " for StgGrupiFile(" + stgGrupiFileId + ")");

		stgGrupiFile.setStatus(StgGrupiFileStatusEnum.FILE_PARSE_IN_PROGRESS);
		stgGrupiFile = stgGrupiFileRepo.save(stgGrupiFile);
		log.info("StgGrupiFile({}).status updated to: {}", stgGrupiFile.getId(), stgGrupiFile.getStatus());
	}

	@AfterJob
	@Transactional(value = "transactionManager", propagation = Propagation.REQUIRES_NEW)
	public void afterJob(JobExecution jobExecution) {
		log.trace("In afterJob");

		var stgGrupiFileId = ParamExtractor.getStgGrupiFileIdRaw(jobExecution.getJobParameters());
		log.debug("stgGrupiFileId: {}", stgGrupiFileId);

		var exitStatus = jobExecution.getExitStatus();
		log.info("Job: [JobExecutionId={}] exitStatus: {} for StgGrupiFile({})", jobExecution.getId(),
				exitStatus.getExitCode(), stgGrupiFileId);

		if (StringUtil.isNumeric(stgGrupiFileId)) {
			var stgGrupiFile = stgGrupiFileRepo.findById(Long.parseLong(stgGrupiFileId)).orElse(null);

			if (stgGrupiFile != null) {
				if (stgGrupiFile.getStatus() == StgGrupiFileStatusEnum.FILE_PARSE_IN_PROGRESS) {
					if (Objects.equals(ExitStatus.COMPLETED, exitStatus)) {
						stgGrupiFile.setStatus(StgGrupiFileStatusEnum.FILE_PARSE_DONE);
					} else {
						stgGrupiFile.setStatus(StgGrupiFileStatusEnum.ERROR);
					}
				}

				stgGrupiFile.setDescription(
						ParamExtractor.getExecutionErrorMessage(jobExecution, stgGrupiFile.getDescription()));

				stgGrupiFile = stgGrupiFileRepo.save(stgGrupiFile);
				log.info("StgGrupiFile({}).status updated to: {}", stgGrupiFile.getId(), stgGrupiFile.getStatus());
			}
		}
	}

}