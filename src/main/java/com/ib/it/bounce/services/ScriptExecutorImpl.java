package com.ib.it.bounce.services;

import org.apache.camel.Exchange;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ScriptExecutorImpl implements ScriptExecutor {

    @Override
    public void executeJob(Exchange exchange) {
        String scriptPath = exchange.getIn().getHeader("scriptPath", String.class);
        String scriptArgs = exchange.getIn().getHeader("scriptArgs", String.class);

        if (scriptPath == null || scriptPath.isEmpty()) {
            setExchangeError(exchange, "Error: Script path is required.");
            return;
        }

        try {
            List<String> command = buildCommand(scriptPath, scriptArgs);
            String result = executeCommand(command);

            exchange.getIn().setHeader("jobSuccess", true);
            exchange.getIn().setBody(result);
        } catch (Exception e) {
            setExchangeError(exchange, "Error executing script: " + e.getMessage());
        }
    }

    private List<String> buildCommand(String scriptPath, String scriptArgs) {
        List<String> command = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            buildWindowsCommand(scriptPath, scriptArgs);
        } else {
            buildLinuxCommand(scriptPath, scriptArgs);
        }

        command.add(scriptPath);
        if (scriptArgs != null && !scriptArgs.isEmpty()) {
            command.addAll(Arrays.asList(scriptArgs.split("\\s+")));
        }

        return command;
    }

    private List<String> buildLinuxCommand(String scriptPath, String scriptArgs) {
        List<String> command = new ArrayList<>();
        if (scriptPath.endsWith(".sh")) {
            command.add("/bin/bash");
        } else if (scriptPath.endsWith(".py")) {
            command.add("python3");
        } else {
            throw new IllegalArgumentException("Unsupported script type for Unix: " + scriptPath);
        }
        return command;
    }

    private List<String> buildWindowsCommand(String scriptPath, String scriptArgs) {
        List<String> command = new ArrayList<>();
        if (scriptPath.endsWith(".bat")) {
            command.add("cmd.exe");
            command.add("/c");
        } else if (scriptPath.endsWith(".ps1")) {
            command.add("powershell");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
        } else if (scriptPath.endsWith(".py")) {
            command.add("python");
        } else {
            throw new IllegalArgumentException("Unsupported script type for Windows: " + scriptPath);
        }
        return command;
    }

    private String executeCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String result = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Script execution failed with exit code: " + exitCode);
        }

        return result;
    }

    private void setExchangeError(Exchange exchange, String errorMessage) {
        exchange.getIn().setHeader("jobSuccess", false);
        exchange.getIn().setBody(errorMessage);
    }
}
