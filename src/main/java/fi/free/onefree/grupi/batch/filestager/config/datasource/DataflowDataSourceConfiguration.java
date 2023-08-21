package fi.free.onefree.grupi.batch.filestager.config.datasource;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This DataSource will be used for accessing CloudDataflow DB.
 */
@Configuration
public class DataflowDataSourceConfiguration {

	@Bean
	@ConfigurationProperties("spring.datasource.dataflow")
	public DataSourceProperties dataflowDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	public DataSource dataflowDataSource() {
		return dataflowDataSourceProperties().initializeDataSourceBuilder().build();
	}

}