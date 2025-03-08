package com.ib.it.bounce.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Ignore null fields in JSON
public class SystemMetrics {



    @JsonProperty("cpu_usage")
    private String cpuUsage;

    @JsonProperty("total_memory")
    private String totalMemory;

    @JsonProperty("free_memory")
    private String freeMemory;

    @JsonProperty("used_memory")
    private String usedMemory;

    @JsonProperty("total_disk_space")
    private String totalDiskSpace;

    @JsonProperty("free_disk_space")
    private String freeDiskSpace;

    @JsonProperty("used_disk_space")
    private String usedDiskSpace;

    @JsonProperty("system_temperature")
    private String systemTemperature;
}

