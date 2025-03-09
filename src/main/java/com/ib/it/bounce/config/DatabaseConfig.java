package com.ib.it.bounce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "monitoring.database")
public class DatabaseConfig {
    private Long timerPeriod;
    private String healthCheckQuery;
    private boolean enabled;

}
