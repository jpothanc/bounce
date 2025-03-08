package com.ib.it.bounce.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SimpleRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:logTimer?period=10000") // Runs every 10 seconds
                .routeId("log-timer-route")
                .log("🚀 Apache Camel Route is running... Timestamp: ${date:now}");
    }
} 