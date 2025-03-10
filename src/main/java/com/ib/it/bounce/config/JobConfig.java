package com.ib.it.bounce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "monitoring.scheduler.jobs")
public class JobConfig {
    private String name;
    private boolean enabled;
    private String cronExpression;
    private String script;
}
