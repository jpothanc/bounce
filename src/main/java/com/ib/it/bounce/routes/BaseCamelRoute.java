package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.MonitoringConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public abstract class BaseCamelRoute extends RouteBuilder {
    protected final CamelContext camelContext;
    protected MemoryCache<String, Object> memoryCache;
    protected final MonitoringConfig monitoringConfig;

    public BaseCamelRoute(CamelContext camelContext, MemoryCache<String, Object> memoryCache, MonitoringConfig monitoringConfig) {
        this.camelContext = camelContext;
        this.memoryCache = memoryCache;
        this.monitoringConfig = monitoringConfig;
    }

    public abstract void configure();

    /**
     * Sends an email or logs the email content based on the `sendEmail` flag.
     */
    protected void sendEmail(Exchange exchange, String recipientEmail) {
        exchange.getMessage().setHeader("sourceRoute", exchange.getFromRouteId());

        exchange.getMessage().setHeader("emailRecipient", recipientEmail);
        exchange.getContext().createProducerTemplate().send("direct:sendEmail", exchange);

    }


}
