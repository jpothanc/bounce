#!/bin/bash

# Define an array of Tomcat installation directories
TOMCAT_SERVERS=(
    "/opt/tomcat1"
    "/opt/tomcat2"
    "/opt/tomcat3"
)

# Function to check if Tomcat is running
is_tomcat_running() {
    local tomcat_home=$1
    ps aux | grep "[t]omcat.*$tomcat_home" > /dev/null
    return $?
}

# Function to restart a single Tomcat server
restart_tomcat() {
    local tomcat_home=$1
    local start_script="$tomcat_home/bin/startup.sh"
    local stop_script="$tomcat_home/bin/shutdown.sh"

    echo "Restarting Tomcat in: $tomcat_home"

    # Stop Tomcat
    if [ -x "$stop_script" ]; then
        echo "Stopping Tomcat..."
        "$stop_script"
        sleep 5
    else
        echo "Warning: Shutdown script not found or not executable: $stop_script"
    fi

    # Ensure Tomcat has fully stopped
    if is_tomcat_running "$tomcat_home"; then
        echo "Tomcat is still running, killing process..."
        pkill -f "$tomcat_home"
        sleep 3
    fi

    # Start Tomcat
    if [ -x "$start_script" ]; then
        echo "Starting Tomcat..."
        "$start_script"
        sleep 5
    else
        echo "Error: Startup script not found or not executable: $start_script"
        return 1
    fi

    # Check if Tomcat restarted successfully
    if is_tomcat_running "$tomcat_home"; then
        echo "Tomcat restarted successfully in: $tomcat_home"
    else
        echo "Error: Tomcat failed to start in: $tomcat_home"
        return 1
    fi
}

# Loop through each Tomcat server and restart it
for TOMCAT_HOME in "${TOMCAT_SERVERS[@]}"; do
    if [ -d "$TOMCAT_HOME" ]; then
        restart_tomcat "$TOMCAT_HOME"
    else
        echo "Error: Tomcat directory not found: $TOMCAT_HOME"
    fi
done

echo "All Tomcat instances processed!"
