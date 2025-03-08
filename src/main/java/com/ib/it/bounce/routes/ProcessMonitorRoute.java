package com.ib.it.bounce.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProcessMonitorRoute extends RouteBuilder {

    @Override
    public void configure() {
//        from("timer:checkProcess?period=30000") // Runs every 30 seconds
//                .routeId("process-monitor")
//                .process(exchange -> {
//                    boolean isRunning = checkProcess("tomcat");
//                    if (!isRunning) {
//                        if(restartProcess("tomcat")) {
//                            exchange.getIn().setHeader("issueResolved", true);
//                        } else {
//                            exchange.getIn().setHeader("issueResolved", false);
//                        }
//                    }
//                    else {
//                        exchange.getIn().setHeader("issueResolved", true);
//                    }
//
//                })
//                .choice()
//                .when(header("issueResolved").isEqualTo(true))
//                .log("🔄 Tomcat was down! Restarted successfully.")
//                .otherwise()
//                .log("✅ Tomcat is running fine.");
    }

    private boolean checkProcess(String processName) throws Exception {
       return false;
    }

    private boolean restartProcess(String processName) throws Exception {
        return true;
    }
}
