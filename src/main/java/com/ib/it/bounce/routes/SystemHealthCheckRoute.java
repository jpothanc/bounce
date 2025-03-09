package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.EmailConfig;
import com.ib.it.bounce.config.MonitoringConfig;
import com.ib.it.bounce.models.SystemMetrics;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SystemHealthCheckRoute extends RouteBuilder {

    private final MonitoringConfig monitoringConfig;
    private MemoryCache<String, Object> memoryCache;
    private final AtomicBoolean emailSent = new AtomicBoolean(false);

    public SystemHealthCheckRoute(MemoryCache<String, Object> memoryCache, MonitoringConfig monitoringConfig) {

        this.memoryCache = memoryCache;
        this.monitoringConfig = monitoringConfig;
    }

    @Override
    public void configure() {

        getContext().getPropertiesComponent().
                addInitialProperty("timerPeriod", String.valueOf(monitoringConfig.getSystemConfig().getTimerPeriod()));

        from("timer:systemInfoTimer?period={{timerPeriod}}")
                .routeId("system-info-route")
                .choice()
                    .when(exchange -> monitoringConfig.getSystemConfig().isEnabled() && monitoringConfig.isWithinMonitoringHours())
                        .to("direct:fetchSystemInfo")
                        .log("System Info: ${body}")
                    .otherwise()
                        .log("🚫 System monitoring is disabled.")
                .end();

        // Route to fetch system metrics
        from("direct:fetchSystemInfo")
                .routeId("fetchSystemInfo-direct")
                .process(exchange -> {
                    SystemMetrics system = fetchSystemMetrics();
                    exchange.getMessage().setBody(system.toString());
                    this.memoryCache.put("systemMetrics", system);
                    checkAndSendMemoryAlert(system, exchange);
                });
    }

    private SystemMetrics fetchSystemMetrics() {
        HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
        GlobalMemory memory = hardware.getMemory();
        CentralProcessor processor = hardware.getProcessor();
        List<OSFileStore> diskStores = new SystemInfo().getOperatingSystem().getFileSystem().getFileStores();

        // Calculate CPU Load
        double cpuLoad = processor.getSystemCpuLoad(1000) * 100;

        // Calculate Memory Usage
        long totalMemory = memory.getTotal();
        long freeMemory = memory.getAvailable();
        long usedMemory = totalMemory - freeMemory;

        // Get Disk Space Info
        File root = new File("/");
        long totalDiskSpace = root.getTotalSpace();
        long freeDiskSpace = root.getFreeSpace();
        long usedDiskSpace = totalDiskSpace - freeDiskSpace;

        // Mock system temperature (OSHI does not support all OS)
        String systemTemperature = "N/A"; // Only available on some Linux systems

        // Build SystemInfo object using Lombok @Builder
        return SystemMetrics.builder()
                .cpuUsage(String.format("%.2f%%", cpuLoad))
                .totalMemory(FormatUtil.formatBytes(totalMemory))
                .freeMemory(FormatUtil.formatBytes(freeMemory))
                .usedMemory(FormatUtil.formatBytes(usedMemory))
                .totalDiskSpace(FormatUtil.formatBytes(totalDiskSpace))
                .freeDiskSpace(FormatUtil.formatBytes(freeDiskSpace))
                .usedDiskSpace(FormatUtil.formatBytes(usedDiskSpace))
                .systemTemperature(systemTemperature)
                .build();
    }

    private void checkAndSendMemoryAlert(SystemMetrics system, Exchange exchange) {
        long freeMemoryMB = 400;

        if (freeMemoryMB < 500 && !emailSent.get()) { // ✅ If free memory is below 500MB
            System.out.println("⚠️ Low Memory Detected! Sending Email Alert...");

            exchange.getMessage().setHeader("sourceRoute", exchange.getFromRouteId());
            exchange.getMessage().setHeader("emailSubject", "⚠️ Low Memory Alert!");
            exchange.getMessage().setHeader("emailBody",
                    "Warning: Free memory is low (" + freeMemoryMB + "MB).\n" +
                            "Please check the system.");
            exchange.getMessage().setHeader("emailRecipient", EmailConfig.TEAM_DEVELOPMENT);

            exchange.getContext().createProducerTemplate().send("direct:sendEmail", exchange);
            emailSent.set(true);
        }
        else if(freeMemoryMB > 500 && emailSent.get()) {
                System.out.println("✅ Memory is back to normal. Sending Recovery Email...");
                exchange.getMessage().setHeader("sourceRoute", exchange.getFromRouteId());
                exchange.getMessage().setHeader("emailSubject", "✅ Memory Recovered");
                exchange.getMessage().setHeader("emailBody", "Memory is back to normal.");
                exchange.getMessage().setHeader("emailRecipient", EmailConfig.TEAM_DEVELOPMENT);

                exchange.getContext().createProducerTemplate().send("direct:sendEmail", exchange);
                emailSent.set(false);
            }
        }
}



