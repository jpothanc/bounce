package com.ib.it.bounce.routes;

import com.ib.it.bounce.config.ProcessItem;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ProcessHealthCheckRoute extends BaseCamelRoute {

    @Override
    public boolean shouldMonitor(Exchange exchange) {
        return monitoringConfig.getProcessConfig().isEnabled() && monitoringConfig.isWithinMonitoringHours();
    }
    @Override
    public void configure() {
        log.info("Configuring {}...", this.getClass().getSimpleName());
        getContext().getPropertiesComponent().
                addInitialProperty("timerPeriod", String.valueOf(monitoringConfig.getDatabaseConfig().getTimerPeriod()));

        setProperty(TIMER_PERIOD_KEY, monitoringConfig.getDatabaseConfig().getTimerPeriod());

        from("timer:processHealthCheck?period={{" + TIMER_PERIOD_KEY + "}}")
                .routeId("process-health-check")
                .choice()
                .when(exchange -> shouldMonitor(exchange))
                    .process(this::checkAndRestartProcesses)
                .otherwise()
                    .log("🚫 Process monitoring is disabled.")
                .end();
    }

    /**
     * ✅ Function to check if processes are running & restart if not
     */
    private void checkAndRestartProcesses(Exchange exchange) {
        List<ProcessItem> processes = monitoringConfig.getProcessConfig().getItems();

        for (ProcessItem process : processes) {
            boolean isRunning = isProcessRunning(process.getName());

            if (isRunning) {
                log.info("✅ Process '{}' is running.", process.getName());
            } else {
                log.warn("⚠️ Process '{}' is NOT running! Attempting to restart...", process.getName());
                restartProcess(process);
            }
        }
    }

    /**
     * ✅ Checks if a process is running based on its name.
     */
    private boolean isProcessRunning(String processName) {
        try {
            String command = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "tasklist"  // Windows command
                    : "ps -e";    // Linux command

            Process process = Runtime.getRuntime().exec(command);
            String output = new String(process.getInputStream().readAllBytes());

            return output.contains(processName);
        } catch (IOException e) {
            log.error("❌ Error checking process '{}': {}", processName, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Attempts to restart a process using its start command.
     */
    private void restartProcess(ProcessItem process) {
        try {
            new ProcessBuilder(process.getStartCommand().split(" "))
                    .directory(new java.io.File(process.getInstallPath()).getParentFile())
                    .start();

            log.info("🚀 Successfully restarted process '{}'.", process.getName());
        } catch (IOException e) {
            log.error("❌ Failed to start process '{}': {}", process.getName(), e.getMessage());
        }
    }
}
