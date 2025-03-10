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
    private final AtomicBoolean emailSent = new AtomicBoolean(false);
    public DatabaseHealthCheckRoute(CamelContext camelContext,
                                    MemoryCache<String, Object> memoryCache,
                                    MonitoringConfig monitoringConfig) {
        super(camelContext, memoryCache, monitoringConfig);
    }
    @Override
    public void configure() {

        log.info("Configuring {}...", this.getClass().getSimpleName());

        getContext().getPropertiesComponent().
                addInitialProperty("timerPeriod", String.valueOf(monitoringConfig.getDatabaseConfig().getTimerPeriod()));
        getContext().getPropertiesComponent().
                addInitialProperty("healthCheckQuery", monitoringConfig.getDatabaseConfig().getHealthCheckQuery());

        from("timer:databaseHealthCheck?period={{timerPeriod}}")
                .routeId("check-mysql-health")
                .choice()
                    .when(exchange -> monitoringConfig.getDatabaseConfig().isEnabled() && monitoringConfig.isWithinMonitoringHours())
                    .log("🔍 Running scheduled MySQL health check...")
                    .doTry()
                        .setBody(simple("{{healthCheckQuery}}")) // Run MySQL check
                        .to("jdbc:dataSource")
                        .setBody(constant("{ \"status\": \"UP\" }"))
                            .process(this::handleDatabaseUp)
                            .log("MySQL is UP!")
                        .doCatch(Exception.class)
                            .process(this::handleDatabaseError) // Classify error type
                            .choice()
                                .when(header("isAuthError").isEqualTo(true))
                                    .process(exchange -> sendEmail(exchange, EmailConfig.TEAM_DEVELOPMENT)) // Send email to Development Team for authentication errors
                                .otherwise()
                                    .process(exchange -> sendEmail(exchange, EmailConfig.TEAM_DATABASE)) // Send email to Database Support Team for other errors
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
