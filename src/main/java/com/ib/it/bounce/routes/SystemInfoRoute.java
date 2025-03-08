package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.models.SystemMetrics;
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

@Component
public class SystemInfoRoute extends RouteBuilder {

    private MemoryCache<String, Object> memoryCache;

    public SystemInfoRoute(MemoryCache<String, Object> memoryCache) {
        this.memoryCache = memoryCache;
    }

    @Override
    public void configure() {
        from("timer:systemInfoTimer?period=1000") // Runs every 10 seconds
                .routeId("system-info-route")
                .to("direct:fetchSystemInfo")
                .log("System Info: ${body}");
        // Route to fetch system metrics
        from("direct:fetchSystemInfo")
                .routeId("fetchSystemInfo-direct")
                .process(exchange -> {
                    SystemMetrics system = fetchSystemMetrics();
                    exchange.getMessage().setBody(system.toString());
                    this.memoryCache.put("systemMetrics", system);
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
}

