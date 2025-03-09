package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.models.ProcessInfo;
import com.sun.management.OperatingSystemMXBean;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class PublishProcessRoute extends RouteBuilder {

    private final Set<String> MONITORED_APPS = Set.of("notepad.exe", "chrome.exe", "firefox.exe");
    private final CamelContext camelContext;
    private  MemoryCache<String, Object> memoryCache;

    public PublishProcessRoute(CamelContext camelContext, MemoryCache<String, Object> memoryCache) {
        this.camelContext = camelContext;
        this.memoryCache = memoryCache;
    }

    private boolean isMonitoredApp(String processName) {
        return MONITORED_APPS.contains(processName);
    }

    @Override
    public void configure() {
        from("timer:statusChecker?period=1000000")
                .routeId("app-status-checker")
                .to("direct:checkStatus");

        from("direct:checkStatus")
                .routeId("check-status-direct")
                .process(exchange -> {
                    List<ProcessInfo> processList = new ArrayList<>();
                    OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    // Iterate over all processes
                    ProcessHandle.allProcesses().filter(process -> isMonitoredApp(process.info().command().orElse("")))
                            .forEach(process -> {
                                ProcessHandle.Info info = process.info();

                                // Use Lombok Builder to construct ProcessInfo objects
                                ProcessInfo processInfo = ProcessInfo.builder()
                                        .processId(String.valueOf(process.pid()))
                                        .processName(info.command().orElse("Unknown"))
                                        .processPath(info.command().orElse("N/A"))
                                        .processDescription("Running process")
                                        .processOwner(info.user().orElse("Unknown"))
                                        .processStatus(process.isAlive() ? "Running" : "Terminated")
                                        .processCpuUsage(String.format("%.2f", osBean.getProcessCpuLoad() * 100) + "%")
                                        .processMemoryUsage(String.format("%.2f", (double) osBean.getFreePhysicalMemorySize() / (1024 * 1024)) + " MB")
                                        .build();

                                // Set start time and uptime if available
                                info.startInstant().ifPresent(start -> {
                                    processInfo.setProcessStartTime(start.toString());
                                    processInfo.setProcessUpTime(Duration.between(start, Instant.now()).toString());
                                });

                                processList.add(processInfo);
                            });

                    memoryCache.put("processList", processList);
                });

    }

}