package com.dissertation.openAPITestGeneration.automatioForRestAPIs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AutomatioForRestApIsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomatioForRestApIsApplication.class, args);
	}

}
