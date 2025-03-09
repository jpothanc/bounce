package com.ib.it.bounce.routes;

import com.ib.it.bounce.config.EmailConfig;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class EmailRoute extends RouteBuilder {

    private final EmailConfig emailConfig;

    public EmailRoute(EmailConfig emailConfig) {
        this.emailConfig = emailConfig;
    }

    @Override
    public void configure() {

        from("direct:sendEmail")
                .routeId("sendEmail-direct")
                .choice()
                    .when(exchange -> emailConfig.isEnabled())
                        .log("📧 Sending Email Alert...")
                        .process(this::sendEmail)
                    .otherwise()
                        .log("Email sending is disabled in configuration.")
                .end();
    }

    /**
     * ✅ Function to handle email processing
     */
    private void sendEmail(Exchange exchange) {
        String source = exchange.getMessage().getHeader("sourceRoute", String.class);
        log.info("📧 Sending Email from: " + source);

        String subject = exchange.getMessage().getHeader("emailSubject", String.class);
        String body = exchange.getMessage().getHeader("emailBody", String.class);
        String team = exchange.getMessage().getHeader("emailRecipient", String.class);
        String recipient = emailConfig.getEmailRecipient(team);

        // Simulate email sending
        System.out.println("📧 Email Sent:");
        System.out.println("  To: " + recipient);
        System.out.println("  Subject: " + subject);
        System.out.println("  Body: " + body);
    }
}
