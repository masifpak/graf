package fi.free.onefree.grupi.batch.filestager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableTask
@ComponentScan({ "fi.free.flj.cloud_storage", "fi.free.onefree" })
public class GrupiFileStagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrupiFileStagerApplication.class, args);
	}

}