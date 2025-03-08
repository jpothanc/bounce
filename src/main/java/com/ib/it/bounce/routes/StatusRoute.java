package com.ib.it.bounce.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@Component
public class StatusRoute extends RouteBuilder {

    private final List<String> MONITORED_APPS = Arrays.asList(
        "user-store",
        "config-service"
    );

    @Override
    public void configure() {
        from("timer:statusChecker?period=30000")
                .routeId("app-status-checker")
                .to("direct:checkStatus");

        from("direct:checkStatus")
                .routeId("check-status-direct")
                .process(exchange -> {
                    StringBuilder result = new StringBuilder("Process List:\n");
                    
                    Process process = Runtime.getRuntime().exec("ps aux");
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream())
                    );
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Skip the header line
                        if (line.contains("USER") && line.contains("PID")) continue;
                        
                        // Check if line contains any of the monitored apps
                        if (MONITORED_APPS.stream().anyMatch(line::contains)) {
                            result.append(line).append("\n");
                        }
                    }
                    
                    if (result.toString().equals("Process List:\n")) {
                        result.append("No monitored applications are currently running.");
                    }
                    
                    exchange.getMessage().setBody(result.toString());
                })
                .log("${body}");
    }
} 