package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.EmailConfig;
import com.ib.it.bounce.config.MonitoringConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DatabaseHealthCheckRoute extends BaseCamelRoute {
    private static final String HEALTH_CHECK_QUERY_KEY = "healthCheckQuery";
    private final AtomicBoolean emailSent = new AtomicBoolean(false);

    @Override
    public boolean shouldMonitor(Exchange exchange) {
        return monitoringConfig.getDatabaseConfig().isEnabled() && monitoringConfig.isWithinMonitoringHours();
    }
    @Override
    public void configure() {

        log.info("Configuring {}...", this.getClass().getSimpleName());

        // Set properties
        setProperty(TIMER_PERIOD_KEY, monitoringConfig.getDatabaseConfig().getTimerPeriod());
        setProperty(HEALTH_CHECK_QUERY_KEY, monitoringConfig.getDatabaseConfig().getHealthCheckQuery());

        from("timer:databaseHealthCheck?period={{" + TIMER_PERIOD_KEY + "}}")
                .routeId("check-mysql-health")
                .choice()
                    .when(exchange -> shouldMonitor(exchange))
                    .log("🔍 Running scheduled MySQL health check...")
                    .doTry()
                        .setBody(simple("{{" + HEALTH_CHECK_QUERY_KEY + "}}"))
                        .to("jdbc:dataSource")
                        .setBody(constant("{ \"status\": \"UP\" }"))
                            .process(this::handleDatabaseUp)
                            .log("MySQL is UP!")
                        .doCatch(Exception.class)
                            .process(this::handleDatabaseError) // Classify error type
                            .choice()
                                .when(header("isAuthError").isEqualTo(true))
                                    .process(exchange -> emailService.send(exchange, EmailConfig.TEAM_DEVELOPMENT))
                                .otherwise()
                                    .process(exchange -> emailService.send(exchange, EmailConfig.TEAM_DATABASE))
                            .end()
                            .setBody(simple("{ \"status\": \"DOWN\" }"))
                .end();
    }

    /**
     * Handles database recovery and sends a one-time "Recovered" email if needed.
     */
    private void handleDatabaseUp(Exchange exchange) {
        if (emailSent.get()) {
            log.info("MySQL is UP! Sending Recovery Email...");
            exchange.getIn().setHeader("emailSubject", "MySQL Service Recovered");
            exchange.getIn().setHeader("emailBody", "MySQL is back online.");
            exchange.getContext().createFluentProducerTemplate().to("direct:sendRecoveryEmail").send();
            emailSent.set(false); // Reset state
        }
    }

    /**
     * Handles database error detection and sends one-time alerts.
     */
    private void handleDatabaseError(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        if (exception == null) return;

        String errorMessage = exception.getMessage();
        boolean isAuthError = errorMessage.contains("Access denied");

        exchange.getIn().setHeader("isAuthError", isAuthError);
        exchange.getIn().setHeader("emailSubject", isAuthError ? "MySQL Authentication Failure" : "MySQL Service Issue");
        exchange.getIn().setHeader("emailBody", "Error: " + errorMessage);

        // Send only one email per issue
        if (!emailSent.get()) {
            log.info("First-time failure detected. Sending alert email.");
            emailSent.set(true);
        } else {
            log.info("MySQL is still down, but email already sent. Skipping email.");
            exchange.setProperty(Exchange.ROUTE_STOP, true); // Stop further processing
        }
    }
}
