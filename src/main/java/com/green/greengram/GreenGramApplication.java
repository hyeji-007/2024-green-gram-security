package com.green.greengram;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class GreenGramApplication {

	public static void main(String[] args) {
		SpringApplication.run(GreenGramApplication.class, args);
	}

}