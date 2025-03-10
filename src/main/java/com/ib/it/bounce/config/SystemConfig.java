package com.ib.it.bounce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "monitoring.system")
public class SystemConfig {
    private boolean enabled;
    private int cpuUsageThreshold;
    private int memoryUsageThreshold;
    private int diskUsageThreshold;
    private Long timerPeriod;
}
