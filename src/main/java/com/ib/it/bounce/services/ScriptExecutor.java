package com.ib.it.bounce.services;

import org.apache.camel.Exchange;

public interface ScriptExecutor {
    void executeJob(Exchange exchange);
}
