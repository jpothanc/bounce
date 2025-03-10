package com.ib.it.bounce.services;

import com.ib.it.bounce.models.SystemMetrics;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class SystemServiceImpl implements SystemService {
    @Override
    public SystemMetrics getSystemMetrics() {
        HardwareAbstractionLayer hardware = new SystemInfo().getHardware();
        GlobalMemory memory = hardware.getMemory();
        CentralProcessor processor = hardware.getProcessor();
        List<OSFileStore> diskStores = new SystemInfo().getOperatingSystem().getFileSystem().getFileStores();
        File root = new File("/");

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        long totalTempSpace = tempDir.getTotalSpace();

        String hostname = "Unknown";
        String ipAddress = "Unknown";

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            hostname = inetAddress.getHostName();
            ipAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Unable to get hostname and IP: " + e.getMessage());
        }

        return SystemMetrics.builder()
                .hostname(hostname)
                .ipAddress(ipAddress)
                .cpuUsage(processor.getSystemCpuLoad(1000) * 100)
                .totalMemory(memory.getTotal())
                .freeMemory(memory.getAvailable())
                .usedMemory(memory.getTotal() - memory.getAvailable())
                .totalDiskSpace(root.getTotalSpace())
                .freeDiskSpace(root.getFreeSpace())
                .totalTempSpace(totalTempSpace)
                .usedDiskSpace(root.getTotalSpace() - root.getFreeSpace())
                .build();
    }
}
