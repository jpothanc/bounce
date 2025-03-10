package com.ib.it.bounce.services;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.EmailConfig;
import com.ib.it.bounce.internal.Utils;
import com.ib.it.bounce.models.Alert;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

@Component
public class EmailService {
    private EmailConfig emailConfig;
    private MemoryCache<String, Object> memoryCache;

    public EmailService(EmailConfig emailConfig, MemoryCache<String, Object> memoryCache) {
        this.emailConfig = emailConfig;
        this.memoryCache = memoryCache;
    }

    public void send(Exchange exchange, String recipientEmail) {
        exchange.getMessage().setHeader("sourceRoute", exchange.getFromRouteId());

        exchange.getMessage().setHeader("emailRecipient", recipientEmail);
        send( exchange);

    }

    public void send(Exchange exchange) {
        String source = exchange.getMessage().getHeader("sourceRoute", String.class);
//        logger.info("📧 Sending Email from: " + source);

        String subject = exchange.getMessage().getHeader("emailSubject", String.class);
        String body = exchange.getMessage().getHeader("emailBody", String.class);
        String team = exchange.getMessage().getHeader("emailRecipient", String.class);
        String recipient = emailConfig.getEmailRecipient(team);

        Alert alert = Alert.builder().alertType("email").alertDetails("Email sent to " + recipient).build();

        memoryCache.put(Utils.createCacheKey("emailAlerts"),alert );

        // Simulate email sending
        System.out.println("📧 Email Sent:");
        System.out.println("  To: " + recipient);
        System.out.println("  Subject: " + subject);
        System.out.println("  Body: " + body);
    }
}
