package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.JobConfig;
import com.ib.it.bounce.config.MonitoringConfig;
import com.ib.it.bounce.config.SchedulerConfig;
import com.ib.it.bounce.services.ScriptExecutor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class ScheduledTasksRoute extends BaseCamelRoute {
    private final SchedulerConfig schedulerConfig;
    @Autowired
    ScriptExecutor scriptExecutor;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

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
            from(getQuartzSchedule(job))
                    .routeId("job-" + job.getName())
                    .setHeader("jobName", constant(job.getName()))
                    .setHeader("scriptPath", constant(schedulerConfig.getJobsPath() + File.separator + job.getScript()))
                    .setHeader("scriptArgs", constant(job.getArgs()))
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
        scriptExecutor.executeJob(exchange);
    }

    private String getQuartzSchedule(JobConfig job) {
        String env = activeProfile;
        if ("dev".equalsIgnoreCase(env)) {
            return "timer://job-" + job.getName() + "?repeatCount=1&delay=0";
        } else {
            return "quartz://job-" + job.getName() + "?cron=" + job.getCronExpression();
        }
    }

}