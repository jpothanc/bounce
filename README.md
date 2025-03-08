# bounce
 App to do health checks and self heal.

```java
from("timer:checkProcess?period=30000") // Run every 30s
    .routeId("process-monitor-workflow")
    .process(exchange -> {
        boolean isRunning = checkIfProcessRunning();
        exchange.getMessage().setHeader("processRunning", isRunning);
    })
    .choice()
        .when(header("processRunning").isEqualTo(false))
            .to("direct:startProcess")
        .otherwise()
            .to("direct:pingProcess");

from("direct:startProcess")
    .log("🔴 Process is NOT running! Starting...")
    .process(exchange -> startProcess())
    .to("direct:pingProcess");

from("direct:pingProcess")
    .log("🔍 Pinging process...")
    .process(exchange -> {
        boolean isPingable = pingProcess();
        exchange.getMessage().setHeader("pingable", isPingable);
    })
    .choice()
        .when(header("pingable").isEqualTo(false))
            .log("⚠️ Process NOT responding. Restarting...")
            .to("direct:startProcess")
        .otherwise()
            .log("✅ Process is running fine.");

```