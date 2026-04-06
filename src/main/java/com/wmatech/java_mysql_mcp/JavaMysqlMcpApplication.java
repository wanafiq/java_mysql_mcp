package com.wmatech.java_mysql_mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JavaMysqlMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaMysqlMcpApplication.class, args);
	}

}
