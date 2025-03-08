package com.ib.it.bounce.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Ignore null fields in JSON
public class ProcessInfo {
    @JsonProperty("id")
    private String processId;

    @JsonProperty("name")
    private String processName;

    @JsonProperty("status")
    private String processStatus;

    @JsonProperty("owner")
    private String processOwner;

    @JsonProperty("cpu_usage")
    private String processCpuUsage;

    @JsonProperty("memory_usage")
    private String processMemoryUsage;

    @JsonProperty("start_time")
    private String processStartTime;

    @JsonProperty("up_time")
    private String processUpTime;

    @JsonProperty("path")
    private String processPath;

    @JsonProperty("description")
    private String processDescription;
}

