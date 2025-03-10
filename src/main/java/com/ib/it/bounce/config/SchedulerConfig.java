package com.ib.it.bounce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.scheduler")
public class SchedulerConfig {
    private boolean enabled;
    private String jobsPath;
    private List<JobConfig> jobs;
}
