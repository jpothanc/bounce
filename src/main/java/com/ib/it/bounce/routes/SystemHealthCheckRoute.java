package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.EmailConfig;
import com.ib.it.bounce.config.MonitoringConfig;
import com.ib.it.bounce.models.SystemMetrics;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SystemHealthCheckRoute extends BaseCamelRoute {
    private final AtomicBoolean emailSent = new AtomicBoolean(false);
    public SystemHealthCheckRoute(CamelContext camelContext,
            MemoryCache<String, Object> memoryCache, MonitoringConfig monitoringConfig) {
        super(camelContext, memoryCache, monitoringConfig);
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
        File root = new File("/");

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        long totalTempSpace = tempDir.getTotalSpace();

        String hostname = "Unknown";
        String ipAddress = "Unknown";

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            hostname = inetAddress.getHostName();
            ipAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Unable to get hostname and IP: " + e.getMessage());
        }

        return SystemMetrics.builder()
                .hostname(hostname)
                .ipAddress(ipAddress)
                .cpuUsage(processor.getSystemCpuLoad(1000) * 100)
                .totalMemory(memory.getTotal())
                .freeMemory(memory.getAvailable())
                .usedMemory(memory.getTotal() - memory.getAvailable())
                .totalDiskSpace(root.getTotalSpace())
                .freeDiskSpace(root.getFreeSpace())
                .totalTempSpace(totalTempSpace)
                .usedDiskSpace(root.getTotalSpace() - root.getFreeSpace())
                .build();
    }

    private void checkAndSendMemoryAlert(SystemMetrics system, Exchange exchange) {
        long freeMemoryMB = system.getFreeMemory() / (1024 * 1024);
        boolean isLowMemory = freeMemoryMB < 500;
        String emailSubject = "";
        String emailBody = "";

        if (isLowMemory && !emailSent.get()) {
            emailSubject =  system.getHostname() + " : Low Memory Alert!";
            emailBody = "Warning: Free memory is low (" + freeMemoryMB + "MB).\n" +
                    "Please check the system.";
            emailSent.set(true);
        } else if (!isLowMemory && emailSent.get()) {
            emailSubject = system.getHostname() + ": Memory Recovered";
            emailBody = "Memory is back to normal.";
            emailSent.set(false);
        }
        exchange.getMessage().setHeader("sourceRoute", exchange.getFromRouteId());
        exchange.getMessage().setHeader("emailSubject", emailSubject);
        exchange.getMessage().setHeader("emailBody", emailBody);
        sendEmail(exchange, EmailConfig.TEAM_DEVELOPMENT);
    }

}



