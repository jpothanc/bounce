package com.ib.it.bounce.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringConfig {

    private String startTime;
    private String endTime;
    private String allowedDays;
    private Long databaseTimerPeriod;

    private LocalTime parsedStartTime;
    private LocalTime parsedEndTime;
    private Set<DayOfWeek> parsedAllowedDays;


    @PostConstruct
    public void init() {
        try {
            this.parsedStartTime = LocalTime.parse(startTime);
            this.parsedEndTime = LocalTime.parse(endTime);
            this.parsedAllowedDays = Arrays.stream(allowedDays.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(DayOfWeek::valueOf)
                    .collect(Collectors.toSet());

            System.out.println("✅ MonitoringConfig initialized: Start=" + parsedStartTime +
                    ", End=" + parsedEndTime + ", Days=" + parsedAllowedDays);
        } catch (Exception e) {
            throw new IllegalArgumentException("❌ Failed to parse MonitoringConfig. Check property values.", e);
        }
    }

    /**
     * Checks if the current time is within the allowed monitoring hours.
     */
    public boolean isWithinMonitoringHours() {
        var now = LocalTime.now();
        var today = DayOfWeek.from(java.time.LocalDate.now());

        return parsedAllowedDays.contains(today) &&
                now.isAfter(parsedStartTime) &&
                now.isBefore(parsedEndTime);
    }
}
