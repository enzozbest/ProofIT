#!/bin/bash

# dev-manager.sh - A script to manage development processes
# This script starts the client development server, captures its output,
# and ensures clean termination of all processes when interrupted.

# Color codes for better readability
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Log directory and files - using absolute paths
BASE_DIR="$(pwd)"
LOG_DIR="$BASE_DIR/logs"
CLIENT_LOG="$LOG_DIR/client-dev.log"
SERVER_LOG="$LOG_DIR/server-dev.log"

# Create log directory if it doesn't exist
mkdir -p "$LOG_DIR"

# Function to display messages with timestamp
log_message() {
    local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    echo -e "${GREEN}[$timestamp]${NC} $1"
}

# Function to cleanup on exit
cleanup() {
    log_message "${RED}Received termination signal. Cleaning up...${NC}"
    
    # Find and kill npm processes related to our development
    log_message "Stopping client development server..."
    pkill -f "npm run dev" || true
    
    log_message "Stopping server processes..."
    
    if [ -n "$SERVER_PID" ]; then
        # Send SIGINT to the process group of start.sh
        kill -SIGINT -$SERVER_PID 2>/dev/null || true
        
        # Give it a moment to handle its own cleanup
        sleep 2
        
        # If it's still running, be more forceful
        if kill -0 $SERVER_PID 2>/dev/null; then
            log_message "Server processes still running. Sending stronger termination signal..."
            kill -SIGTERM -$SERVER_PID 2>/dev/null || true
            sleep 1
            
            # Last resort - force kill
            if kill -0 $SERVER_PID 2>/dev/null; then
                log_message "Server processes still running. Force killing..."
                kill -9 -$SERVER_PID 2>/dev/null || true
            fi
        fi
        
        # Additionally find any lingering processes that might have been started by start.sh
        # Use pgrep to find processes whose parent is start.sh
        for child_pid in $(pgrep -P $SERVER_PID 2>/dev/null); do
            log_message "Stopping child process with PID: $child_pid"
            kill -SIGINT $child_pid 2>/dev/null || true
            sleep 1
            kill -9 $child_pid 2>/dev/null || true
        done
    fi
    
    # Kill any other background processes started by this script
    jobs -p | xargs -r kill 2>/dev/null || true
    
    log_message "All processes have been terminated."
    log_message "Exiting dev-manager script."
    exit 0
}

# Trap signals for clean shutdown
trap cleanup SIGINT SIGTERM

# Function to monitor a log file and display new content
monitor_log() {
    local log_file=$1
    local prefix=$2
    
    # Create the file if it doesn't exist
    touch "$log_file"
    
    # Use tail to follow the file and display new content
    tail -f "$log_file" | while read -r line; do
        echo -e "${prefix} ${line}"
    done &
}

# Main execution starts here
log_message "${BLUE}Starting development environment...${NC}"

# Display the current directory
log_message "Current directory: $(pwd)"

# Check if the client directory exists
if [ ! -d "one-day-poc-client" ]; then
    log_message "${RED}Error: 'one-day-poc-client' directory not found!${NC}"
    log_message "Please make sure you're running this script from the correct location."
    exit 1
fi

# Start the client development server
log_message "${YELLOW}Starting client development server...${NC}"

# Check if client starter script exists
CLIENT_STARTER="$BASE_DIR/one-day-poc-client/start.sh"
if [ ! -f "$CLIENT_STARTER" ]; then
    log_message "${RED}Error: Client starter script not found at $CLIENT_STARTER${NC}"
    log_message "${RED}Please create the client starter script before running this manager.${NC}"
    exit 1
fi

# Make sure the client starter script is executable
chmod +x "$CLIENT_STARTER"

# Run the client starter script and redirect output to log file
$CLIENT_STARTER > "$CLIENT_LOG" 2>&1 &
CLIENT_PID=$!

# Return to the base directory
cd "$BASE_DIR"

# Start the server using start.sh
log_message "${YELLOW}Starting server using start.sh...${NC}"
SERVER_DIR="$BASE_DIR/one-day-poc-server"
SERVER_SCRIPT="$SERVER_DIR/start.sh"

if [ -d "$SERVER_DIR" ] && [ -f "$SERVER_SCRIPT" ]; then
    cd "$SERVER_DIR"
    
    # Make sure start.sh is executable
    chmod +x start.sh
    
    # Run start.sh in its own process group and redirect output to log file
    # The 'setsid' command creates a new session and process group
    setsid ./start.sh > "$SERVER_LOG" 2>&1 &
    SERVER_PID=$!
    
    # Store the process group ID for later cleanup
    SERVER_PGID=$SERVER_PID
    
    cd "$BASE_DIR"
else
    log_message "${RED}Warning: Could not find $SERVER_SCRIPT${NC}"
    log_message "Continuing without starting server. Please check the project structure."
    SERVER_PID=""
fi

# Start monitoring the log files
log_message "Monitoring client output (from $CLIENT_LOG):"

# Add a small delay to ensure log files exist before monitoring
sleep 1
monitor_log "$CLIENT_LOG" "${BLUE}[CLIENT]${NC}"

# Monitor server logs if server was started
if [ -n "$SERVER_PID" ]; then
    log_message "Monitoring server output (from $SERVER_LOG):"
    monitor_log "$SERVER_LOG" "${YELLOW}[SERVER]${NC}"
fi

# Display process information
log_message "Client development server running with PID: $CLIENT_PID"
if [ -n "$SERVER_PID" ]; then
    log_message "Server running with PID: $SERVER_PID"
fi
log_message "${GREEN}All processes started successfully!${NC}"
log_message "Press Ctrl+C to stop all processes and exit."

# Keep the script running until interrupted
# This allows the log monitoring to continue
while true; do
    sleep 1
    
    # Check if the client process is still running
    if ! kill -0 $CLIENT_PID 2>/dev/null; then
        log_message "${RED}Client development server has stopped unexpectedly.${NC}"
        log_message "Checking the last few lines of the log:"
        tail -n 10 "$CLIENT_LOG"
        cleanup
        break
    fi
    
    # Check if the server process is still running (if it was started)
    if [ -n "$SERVER_PID" ] && ! kill -0 $SERVER_PID 2>/dev/null; then
        log_message "${RED}Server has stopped unexpectedly.${NC}"
        log_message "Checking the last few lines of the log:"
        tail -n 10 "$SERVER_LOG"
        cleanup
        break
    fi
done
