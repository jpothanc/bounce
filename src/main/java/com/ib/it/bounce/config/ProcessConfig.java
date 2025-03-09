package com.ib.it.bounce.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "monitoring.process")
public class ProcessConfig {
    private boolean enabled;
    private Long timerPeriod;
    private List<ProcessItem> items;  // ✅ List of processes

    @PostConstruct
    public void init() {
        System.out.println("🔍 ProcessConfig Initialized:");
        System.out.println("  Enabled = " + enabled);
        System.out.println("  Timer Period = " + timerPeriod + " ms");
        System.out.println("  Processes = " + items);

        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("❌ Process list is empty! Check application.yaml");
        }
    }
}
