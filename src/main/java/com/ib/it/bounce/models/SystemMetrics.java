package com.ib.it.bounce.models;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import oshi.util.FormatUtil;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMetrics {

    @JsonProperty("hostname")
    private String hostname;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("cpu_usage")
    private double cpuUsage;

    @JsonProperty("total_memory")
    private long totalMemory;

    @JsonProperty("free_memory")
    private long freeMemory;

    @JsonProperty("used_memory")
    private long usedMemory;

    @JsonProperty("total_disk_space")
    private long totalDiskSpace;

    @JsonProperty("free_disk_space")
    private long freeDiskSpace;

    @JsonProperty("used_disk_space")
    private long usedDiskSpace;

    @JsonProperty("total_temp_space")
    private long totalTempSpace;

    @JsonGetter("cpu_usage")
    public String getCpuUsageAsString() {
        return String.format("%.2f%%", cpuUsage);
    }

    @JsonGetter("total_memory")
    public String getTotalMemoryAsString() {
        return FormatUtil.formatBytes(totalMemory);
    }

    @JsonGetter("free_memory")
    public String getFreeMemoryAsString() {
        return FormatUtil.formatBytes(freeMemory);
    }

    @JsonGetter("used_memory")
    public String getUsedMemoryAsString() {
        return FormatUtil.formatBytes(usedMemory);
    }

    @JsonGetter("total_disk_space")
    public String getTotalDiskSpaceAsString() {
        return FormatUtil.formatBytes(totalDiskSpace);
    }

    @JsonGetter("free_disk_space")
    public String getFreeDiskSpaceAsString() {
        return FormatUtil.formatBytes(freeDiskSpace);
    }

    @JsonGetter("used_disk_space")
    public String getUsedDiskSpaceAsString() {
        return FormatUtil.formatBytes(usedDiskSpace);
    }
    @JsonGetter("total_temp_space")
    public String getTotalTempSpaceAsString() {
        return FormatUtil.formatBytes(totalTempSpace);
    }
}
