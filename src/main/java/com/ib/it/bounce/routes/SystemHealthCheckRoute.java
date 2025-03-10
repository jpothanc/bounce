package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.EmailConfig;
import com.ib.it.bounce.config.MonitoringConfig;
import com.ib.it.bounce.models.SystemMetrics;
import com.ib.it.bounce.services.SystemService;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SystemHealthCheckRoute extends BaseCamelRoute {
    private final AtomicBoolean emailSent = new AtomicBoolean(false);
    @Autowired
    SystemService systemService;
    public SystemHealthCheckRoute(CamelContext camelContext,
            MemoryCache<String, Object> memoryCache, MonitoringConfig monitoringConfig) {
        super(camelContext, memoryCache, monitoringConfig);
    }

    @Override
    public boolean shouldMonitor(Exchange exchange) {
        return monitoringConfig.getSystemConfig().isEnabled() && monitoringConfig.isWithinMonitoringHours();
    }

    @Override
    public void configure() {
        log.info("Configuring {}...", this.getClass().getSimpleName());
        getContext().getPropertiesComponent().
                addInitialProperty("timerPeriod", String.valueOf(monitoringConfig.getSystemConfig().getTimerPeriod()));

        from("timer:systemInfoTimer?period={{timerPeriod}}")
                .routeId("system-info-route")
                .choice()
                    .when(exchange -> shouldMonitor(exchange))
                        .to("direct:fetchSystemInfo")
                        .log("System Info: ${body}")
                    .otherwise()
                        .log("🚫 System monitoring is disabled.")
                .end();

        // Route to fetch system metrics
        from("direct:fetchSystemInfo")
                .routeId("fetchSystemInfo-direct")
                .process(exchange -> {
                    SystemMetrics system = systemService.getSystemMetrics();
                    exchange.getMessage().setBody(system.toString());
                    this.memoryCache.put("systemMetrics", system);
                    checkAndSendSystemAlerts(system, exchange);
                });
    }

    private void checkAndSendSystemAlerts(SystemMetrics system, Exchange exchange) {
        long freeDiskGB = system.getFreeDiskSpace() / (1024 * 1024 * 1024);
        double cpuUsage = system.getCpuUsage();
        String hostname = system.getHostname();

        double freeMemoryPercentage = ((double) system.getFreeMemory() / system.getTotalMemory()) * 100;
        long freeMemoryMB = system.getFreeMemory() / (1024 * 1024);
        double freeDiskPercentage = ((double) system.getFreeDiskSpace() / system.getTotalDiskSpace()) * 100;

        boolean isLowMemory = freeMemoryPercentage > monitoringConfig.getSystemConfig().getMemoryUsageThreshold();
        boolean isHighCpu = cpuUsage > monitoringConfig.getSystemConfig().getCpuUsageThreshold();
        boolean isLowDisk = freeDiskPercentage > monitoringConfig.getSystemConfig().getDiskUsageThreshold();


        StringBuilder alertBody = new StringBuilder();

        if (isLowMemory) {
            log.warn("Low Memory: Free memory is low ({} MB).", freeMemoryMB);
            alertBody.append("Low Memory: Free memory is low (").append(freeMemoryMB).append(" MB).\n");
        }
        if (isHighCpu) {
            log.warn("High CPU Usage: CPU usage is high ({}%).", cpuUsage);
            alertBody.append("High CPU Usage: CPU usage is high (").append(cpuUsage).append("%).\n");
        }
        if (isLowDisk) {
            log.warn("Low Disk Space: Free disk space is low ({} GB).", freeDiskGB);
            alertBody.append("Low Disk Space: Free disk space is low (").append(freeDiskGB).append(" GB).\n");
        }

        if (alertBody.length() > 0 && !emailSent.get()) {
            String subject = hostname + " : System Alerts!";
            sendAlert(exchange, subject, alertBody.toString());
            emailSent.set(true);
        } else if (alertBody.length() == 0 && emailSent.get()) {
            sendAlert(exchange, hostname + ": System Recovered", "All system metrics are back to normal.");
            emailSent.set(false);
        }
    }

    private void sendAlert(Exchange exchange, String subject, String body) {
        exchange.getMessage().setHeaders(Map.of(
                "sourceRoute", exchange.getFromRouteId(),
                "emailSubject", subject,
                "emailBody", body
        ));
        System.out.println("📧 Sending Email: " + subject);
        sendEmail(exchange, EmailConfig.TEAM_DEVELOPMENT);
    }

}



