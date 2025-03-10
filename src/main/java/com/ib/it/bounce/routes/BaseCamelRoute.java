package com.ib.it.bounce.routes;

import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.config.MonitoringConfig;
import com.ib.it.bounce.services.EmailService;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseCamelRoute extends RouteBuilder {
    protected static final String TIMER_PERIOD_KEY = "timerPeriod";
    protected CamelContext camelContext;
    protected MemoryCache<String, Object> memoryCache;
    protected MonitoringConfig monitoringConfig;
    protected EmailService emailService;

    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Autowired
    public void setMemoryCache(MemoryCache<String, Object> memoryCache) {
        this.memoryCache = memoryCache;
    }

    @Autowired
    public void setMonitoringConfig(MonitoringConfig monitoringConfig) {
        this.monitoringConfig = monitoringConfig;
    }

    @Autowired
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public CamelContext getContext() {
        return camelContext;
    }

    public abstract void configure();

    protected void setProperty(String key, Object value) {
        getContext().getPropertiesComponent().addInitialProperty(key, String.valueOf(value));
    }

    public abstract boolean shouldMonitor(Exchange exchange);

}
