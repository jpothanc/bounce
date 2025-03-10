package com.ib.it.bounce;

import com.ib.it.bounce.config.MonitoringConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class BounceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BounceApplication.class, args);
	}

}
