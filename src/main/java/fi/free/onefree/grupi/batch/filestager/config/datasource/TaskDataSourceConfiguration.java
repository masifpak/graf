package fi.free.onefree.grupi.batch.filestager.config.datasource;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This DataSource will be used for for accessing application DB.
 */
@Configuration
@EnableJpaRepositories(basePackages = { "fi.free.onefree.repo", "fi.free.onefree.grupi.repo" },
		entityManagerFactoryRef = "taskEntityManager", transactionManagerRef = "taskTransactionManager")
public class TaskDataSourceConfiguration {

	@Primary
	@Bean
	@ConfigurationProperties("spring.datasource.task")
	public DataSourceProperties taskDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Primary
	@Bean
	public DataSource taskDataSource() {
		return taskDataSourceProperties().initializeDataSourceBuilder().build();
	}

	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean taskEntityManager() {
		var entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(taskDataSource());
		entityManagerFactoryBean.setPackagesToScan("fi.free.onefree.entity", "fi.free.onefree.grupi.entity");

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);

		return entityManagerFactoryBean;
	}

	@Primary
	@Bean
	public PlatformTransactionManager taskTransactionManager() {
		var transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(taskEntityManager().getObject());

		return transactionManager;
	}

}