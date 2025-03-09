package com.ib.it.bounce.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DatabaseHealthCheckRoute extends RouteBuilder {
    private static final boolean SEND_EMAIL = false;
    private final AtomicBoolean emailSent = new AtomicBoolean(false);

    @Override
    public void configure() {
        // Run every 1 minute
        from("timer:databaseHealthCheck?period=60000")
                .routeId("check-mysql-health")
                .doTry()
                .setBody(constant("SELECT 1")) // Run MySQL check
                .to("jdbc:dataSource")
                .setBody(constant("{ \"status\": \"UP\" }"))
                .process(this::handleDatabaseUp)
                .log("✅ MySQL is UP!")
                .doCatch(Exception.class)
                .process(this::handleDatabaseError) // Classify error type
                .choice()
                .when(header("isAuthError").isEqualTo(true))
                .to("direct:sendEmailToDev") // Send email to Development Team for auth failures
                .otherwise()
                .to("direct:sendEmailToDBSupport") // Send email to Database Support Team for other issues
                .end()
                .setBody(simple("{ \"status\": \"DOWN\" }"))
                .end();

        // Email route for Development Team (Authentication Errors)
        from("direct:sendEmailToDev")
                .process(exchange -> sendOrLogEmail(exchange, "dev-team@example.com", SEND_EMAIL));

        // Email route for Database Support Team (Other Errors)
        from("direct:sendEmailToDBSupport")
                .process(exchange -> sendOrLogEmail(exchange, "db-support@example.com", SEND_EMAIL));

        // Recovery email route
        from("direct:sendRecoveryEmail")
                .process(exchange -> sendOrLogEmail(exchange, "dev-team@example.com", SEND_EMAIL)); // Recovery emails go to Dev Team
    }

    /**
     * Handles database recovery and sends a one-time "Recovered" email if needed.
     */
    private void handleDatabaseUp(Exchange exchange) {
        if (emailSent.get()) {
            log.info("✅ MySQL is UP! Sending Recovery Email...");
            exchange.getIn().setHeader("emailSubject", "✅ MySQL Service Recovered");
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
        exchange.getIn().setHeader("emailSubject", isAuthError ? "⚠️ MySQL Authentication Failure" : "🚨 MySQL Service Issue");
        exchange.getIn().setHeader("emailBody", "Error: " + errorMessage);

        // Send only one email per issue
        if (!emailSent.get()) {
            log.info("🚨 First-time failure detected. Sending alert email.");
            emailSent.set(true); // Mark email as sent
        } else {
            log.info("❌ MySQL is still down, but email already sent. Skipping email.");
            exchange.setProperty(Exchange.ROUTE_STOP, true); // Stop further processing
        }
    }

    /**
     * Sends an email or logs the email content based on the `sendEmail` flag.
     */
    private void sendOrLogEmail(Exchange exchange, String recipientEmail, boolean sendEmail) {
        String subject = exchange.getIn().getHeader("emailSubject", String.class);
        String body = exchange.getIn().getHeader("emailBody", String.class);

        if(subject == null || body == null) {
            log.error("Email subject or body is null. Cannot send email.");
            return;
        }

        if (sendEmail) {
            log.info("📧 Sending Real Email to {}", recipientEmail);
            exchange.getIn().setHeader("to", recipientEmail);
            exchange.getIn().setHeader("subject", subject);
            exchange.getIn().setBody(body);
            exchange.getContext().createProducerTemplate()
                    .send("smtp://smtp.example.com?username=your-email@example.com&password=yourpassword", exchange);
        } else {
            log.info("\n📧 Simulated Email Notification 📧");
            log.info("📢 Subject: {}", subject);
            log.info("📨 Recipient: {}", recipientEmail);
            log.info("✉️ Body: {}", body);
        }
    }
}
