package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.MonitoringConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public abstract class BaseCamelRoute extends RouteBuilder {
    protected static final String TIMER_PERIOD_KEY = "timerPeriod";
    protected final CamelContext camelContext;
    protected final MemoryCache<String, Object> memoryCache;
    protected final MonitoringConfig monitoringConfig;

    public BaseCamelRoute(CamelContext camelContext, MemoryCache<String, Object> memoryCache,
                          MonitoringConfig monitoringConfig) {
        this.camelContext = camelContext;
        this.memoryCache = memoryCache;
        this.monitoringConfig = monitoringConfig;
    }

    public abstract void configure();

    protected void sendEmail(Exchange exchange, String recipientEmail) {
        exchange.getMessage().setHeader("sourceRoute", exchange.getFromRouteId());

        exchange.getMessage().setHeader("emailRecipient", recipientEmail);
        exchange.getContext().createProducerTemplate().send("direct:sendEmail", exchange);

    }
    protected void setProperty(String key, Object value) {
        getContext().getPropertiesComponent().addInitialProperty(key, String.valueOf(value));
    }

    public abstract boolean shouldMonitor(Exchange exchange);

}
