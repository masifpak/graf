package fi.free.onefree.grupi.batch.filestager.config;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

/**
 * Custom implementation for JobParametersIncrementer to prevent retrieving previous job execution parameters
 */
public class GrupiRunIdIncrementer extends RunIdIncrementer {

	private static final String DEFAULT_KEY = "run.id";
	private static final long DEFAULT_ID = 1;

	@Override
	public JobParameters getNext(JobParameters parameters) {
		JobParameters params = new JobParameters();
		JobParameter runIdParameter = parameters.getParameters().get(DEFAULT_KEY);
		long id = DEFAULT_ID;
		if (runIdParameter != null) {
			try {
				id = Long.parseLong(runIdParameter.getValue().toString()) + 1;
			} catch (NumberFormatException exception) {
				throw new IllegalArgumentException("Invalid value for parameter " + DEFAULT_KEY, exception);
			}
		}
		return new JobParametersBuilder(params).addLong(DEFAULT_KEY, id).toJobParameters();
	}
}
