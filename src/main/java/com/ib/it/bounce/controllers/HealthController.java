package com.ib.it.bounce.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin(origins = "*") // Allow all origins
public class HealthController {

    @GetMapping("/check")
    public String healthCheck() {
        return "Service is up and running";
    }
}
