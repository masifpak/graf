package fi.free.onefree.grupi.batch.filestager.util;

import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.util.Assert;

import fi.free.flj.util.StringUtil;
import fi.free.onefree.grupi.batch.filestager.GrupiFileStagerConstants;
import fi.free.onefree.grupi.batch.filestager.exception.GrupiBatchException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParamExtractor {

	private static boolean validateJobParameters(JobParameters jobParameters) {
		if (log.isDebugEnabled()) {
			log.debug("JobParameters: {}", jobParameters);
		}

		Assert.notNull(jobParameters, GrupiFileStagerConstants.JOB_PARAMS_NULL);
		Assert.notEmpty(jobParameters.getParameters(), GrupiFileStagerConstants.JOB_PARAMS_EMPTY);

		return true;
	}

	public static boolean validateJobParamStgGrupiFileId(JobParameters jobParameters) {
		validateJobParameters(jobParameters);

		var strStgGrupiFileId = jobParameters.getString(GrupiFileStagerConstants.STG_GRUPI_FILE_ID);
		log.debug("JobParameter STG_GRUPI_FILE_ID: {}", strStgGrupiFileId);

		Assert.hasText(strStgGrupiFileId, GrupiFileStagerConstants.NOT_FOUND_STG_GRUPI_FILE_ID);
		Assert.isTrue(StringUtil.isNumeric(strStgGrupiFileId),
				GrupiFileStagerConstants.INVALID_FORMAT_STG_GRUPI_FILE_ID);

		return true;
	}

	public static Long getStgGrupiFileId(JobParameters jobParameters) {
		// all validations happened by the time this method is called
		return Long.parseLong(getStgGrupiFileIdRaw(jobParameters));
	}

	public static String getStgGrupiFileIdRaw(JobParameters jobParameters) {
		return jobParameters.getString(GrupiFileStagerConstants.STG_GRUPI_FILE_ID);
	}

	public static Long getStgGrupiFileId(StepExecution stepExecution) {
		return getStgGrupiFileId(stepExecution.getJobParameters());
	}

	public static Long getStgGrupiFileId(StepContext stepContext) {
		return getStgGrupiFileId(stepContext.getStepExecution());
	}

	public static Long getStgGrupiFileId(JobExecution jobExecution) {
		return getStgGrupiFileId(jobExecution.getJobParameters());
	}

	public static String getJobExecutionMessage(final Long jobExecutionId, final String oldDescription) {
		return new StringBuilder().append(StringUtil.SQUARE_BRACKET_OPEN)
				.append(GrupiFileStagerConstants.STR_JOB_EXECUTION_ID)
				.append(StringUtil.EQUAL)
				.append(jobExecutionId)
				.append(StringUtil.SQUARE_BRACKET_CLOSE)
				.append(StringUtil.BLANK_SPACE)
				.append(StringUtil.isNullOrBlank(oldDescription) ? StringUtil.BLANK
						: StringUtil.NEW_LINE + oldDescription)
				.toString();
	}

	@SuppressWarnings("unused")
	public static String getExecutionErrorMessage(StepExecution stepExecution, String oldDescription) {
		return getExecutionErrorMessage(stepExecution.getJobExecution(), oldDescription);
	}

	public static String getExecutionErrorMessage(JobExecution jobExecution, String oldDescription) {
		var exceptionMessages = jobExecution.getAllFailureExceptions()
				.stream()
				.map(ParamExtractor::getExceptionMessage)
				.collect(Collectors.joining(StringUtil.SEMICOLON));

		if (StringUtil.isNotNullAndNotBlank(exceptionMessages)) {
			return new StringBuilder().append(StringUtil.SQUARE_BRACKET_OPEN)
					.append(GrupiFileStagerConstants.STR_JOB_EXECUTION_ID)
					.append(StringUtil.EQUAL)
					.append(jobExecution.getId())
					.append(StringUtil.SQUARE_BRACKET_CLOSE)
					.append(StringUtil.BLANK_SPACE)
					.append(exceptionMessages)
					.append(StringUtil.isNullOrBlank(oldDescription) ? StringUtil.BLANK
							: StringUtil.NEW_LINE + oldDescription)
					.toString();
		} else {
			return oldDescription;
		}
	}

	@SuppressWarnings("unused")
	private static String getExceptionStackMessage(Throwable e) {
		return new StringBuilder(getExceptionMessage(e))
				.append(e.getCause() == null ? StringUtil.BLANK
						: StringUtil.SEMICOLON + StringUtil.BLANK_SPACE + GrupiFileStagerConstants.STR_CAUSE
								+ StringUtil.COLON + StringUtil.BLANK_SPACE + getExceptionMessage(e.getCause()))
				.toString();
	}

	private static String getExceptionMessage(Throwable e) {
		var exceptionMessage = new StringBuilder();

		if (e.getClass() == GrupiBatchException.class) {
			exceptionMessage.append(e.getMessage());
		} else if (e.getClass() == IllegalArgumentException.class) {
			exceptionMessage.append(getExceptionMessageOrClassName(getRootOrCurrentException(e)));
		} else {
			exceptionMessage.append(getExceptionMessageWithClassName(e));
		}

		return exceptionMessage.toString();
	}

	private static Throwable getRootOrCurrentException(final Throwable e) {
		Throwable root = ExceptionUtils.getRootCause(e);
		return root != null ? root : e;
	}

	private static String getExceptionMessageWithClassName(final Throwable e) {
		return e == null ? null
				: new StringBuilder().append(e.getClass().getSimpleName())
						.append(StringUtil.isNullOrBlank(e.getMessage()) ? StringUtil.BLANK
								: StringUtil.COLON + StringUtil.BLANK_SPACE + e.getMessage())
						.toString();
	}

	private static String getExceptionMessageOrClassName(final Throwable e) {
		return e == null ? null
				: StringUtil.isNotNullAndNotBlank(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
	}

}