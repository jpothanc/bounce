package com.ib.it.bounce.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @GetMapping("/apps")
    public String getRunningApps() {
        // Direct endpoint to trigger the status check
        return producerTemplate.requestBody("direct:checkStatus", null, String.class);
    }
} 