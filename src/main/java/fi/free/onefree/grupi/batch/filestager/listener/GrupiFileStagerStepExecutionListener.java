package fi.free.onefree.grupi.batch.filestager.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import fi.free.onefree.grupi.batch.filestager.util.ParamExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener for Grupi file staging step execution events.
 */
@Slf4j
@RequiredArgsConstructor
public class GrupiFileStagerStepExecutionListener implements StepExecutionListener {

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.trace("In beforeStep");
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.trace("In afterStep");

		var stgGrupiFileId = ParamExtractor.getStgGrupiFileId(stepExecution);
		log.debug("stgGrupiFileId: {}", stgGrupiFileId);

		var exitStatus = stepExecution.getExitStatus();
		log.info("Step: [JobExecutionId={}, name={}] exitStatus: {} for StgGrupiFile({})",
				stepExecution.getJobExecutionId(), stepExecution.getStepName(), exitStatus.getExitCode(),
				stgGrupiFileId);

		return exitStatus;
	}

}