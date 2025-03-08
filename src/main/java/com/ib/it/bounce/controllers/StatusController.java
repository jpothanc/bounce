package com.ib.it.bounce.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.it.bounce.cache.MemoryCache;
import com.ib.it.bounce.models.ProcessInfo;
import com.ib.it.bounce.models.SystemMetrics;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    @Autowired
    private ProducerTemplate producerTemplate;
    private MemoryCache<String, Object> memoryCache;

    public StatusController(MemoryCache<String, Object> memoryCache) {
        this.memoryCache = memoryCache;
    }

    @GetMapping("/apps")
    public ResponseEntity<List<ProcessInfo>> getRunningApps() {
        // Direct endpoint to trigger the status check
        return this.memoryCache.get("processList") != null ? ResponseEntity.ok((List<ProcessInfo>) this.memoryCache.get("processList")) :
                ResponseEntity.ok(List.of(ProcessInfo.builder().build()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetrics> getSystemMetrics() {
        // Direct endpoint to trigger the status check
        return this.memoryCache.get("systemMetrics") != null ? ResponseEntity.ok((SystemMetrics) this.memoryCache.get("systemMetrics")) :
                ResponseEntity.ok(SystemMetrics.builder().build());

    }
} 