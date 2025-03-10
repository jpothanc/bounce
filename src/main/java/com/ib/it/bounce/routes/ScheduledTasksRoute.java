package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.JobConfig;
import com.ib.it.bounce.config.MonitoringConfig;
import com.ib.it.bounce.config.SchedulerConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class ScheduledTasksRoute extends BaseCamelRoute {
    private final SchedulerConfig schedulerConfig;

    public ScheduledTasksRoute(CamelContext camelContext,
                               MemoryCache<String, Object> memoryCache,
                               MonitoringConfig monitoringConfig) {
        super(camelContext, memoryCache, monitoringConfig);
        this.schedulerConfig = monitoringConfig.getSchedulerConfig();
    }

    @Override
    public void configure() {
        log.info("Configuring {}...", this.getClass().getSimpleName());
        if (!schedulerConfig.isEnabled()) {
            log.info("Scheduler is disabled. No jobs will be scheduled.");
            return;
        }

        List<JobConfig> jobs = schedulerConfig.getJobs();
        for (JobConfig job : jobs) {
            if (!job.isEnabled()) {
                log.info("Job '{}' is disabled. Skipping...", job.getName());
                continue;
            }
            from("quartz://job-" + job.getName() + "?cron=" + job.getCronExpression())
                    .routeId("job-" + job.getName())
                    .setHeader("jobName", constant(job.getName()))
                    .setHeader("scriptPath", constant(schedulerConfig.getJobsPath() + File.separator + job.getScript()))
                    .log("Running Job: ${header.jobName}")
                    .process(this::executeJob)
                    .choice()
                        .when(header("jobSuccess").isEqualTo(true))
                            .log("Job ${header.jobName} completed successfully: ${body}")
                        .otherwise()
                            .log("Job ${header.jobName} failed: ${body}")
                    .end();
        }

    }
    private void executeJob(Exchange exchange) {
        String scriptPath = exchange.getIn().getHeader("scriptPath", String.class);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", scriptPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            String result = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            exchange.getIn().setHeader("jobSuccess", exitCode == 0);
            exchange.getIn().setBody(result);
        } catch (Exception e) {
            exchange.getIn().setHeader("jobSuccess", false);
            exchange.getIn().setBody("Error executing script: " + e.getMessage());
        }
    }
}