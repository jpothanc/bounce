package com.ib.it.bounce.routes;

import com.ib.it.bounce.config.JobConfig;
import com.ib.it.bounce.config.SchedulerConfig;
import com.ib.it.bounce.services.ScriptExecutor;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class ScheduledTasksRoute extends BaseCamelRoute {
    private static final int THREAD_POOL_SIZE = 5;
    private SchedulerConfig schedulerConfig;
    private  ScriptExecutor scriptExecutor;

    @Autowired
    public void setSchedulerConfig(SchedulerConfig schedulerConfig) {
        this.schedulerConfig = schedulerConfig;
    }
    @Autowired
    public void setScriptExecutor(ScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;


    @Override
    public boolean shouldMonitor(Exchange exchange) {
        return schedulerConfig.isEnabled();
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
                    .threads(THREAD_POOL_SIZE)  // Enables parallel execution
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