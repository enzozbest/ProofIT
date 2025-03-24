#!/bin/bash

# client-starter.sh - Script to start the client application
# This script checks if dependencies are installed before starting the client

# Color codes for better readability
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to display messages with timestamp
log_message() {
    local timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    echo -e "${GREEN}[$timestamp]${NC} $1"
}

# Function to check if node_modules exists and has content
check_dependencies() {
    # Check if node_modules directory exists
    if [ ! -d "node_modules" ]; then
        return 1 # Dependencies not installed
    fi

    # Check if node_modules has content (not empty)
    if [ -z "$(ls -A node_modules 2>/dev/null)" ]; then
        return 1 # Dependencies not installed or directory empty
    fi

    # Check if package.json exists
    if [ ! -f "package.json" ]; then
        log_message "${RED}Error: package.json not found. Are you in the correct directory?${NC}"
        exit 1
    fi

    # Get the number of dependencies from package.json
    local dep_count=$(grep -c '"dependencies"' package.json)
    local dev_dep_count=$(grep -c '"devDependencies"' package.json)
    
    # Get rough count of installed modules (just top-level directories)
    local installed_count=$(find node_modules -maxdepth 1 -type d | wc -l)
    
    # If the number of installed packages seems significantly lower than dependencies,
    # assume the installation is incomplete
    if [ $installed_count -lt 5 ]; then
        log_message "${YELLOW}Detected potentially incomplete installation (only $installed_count packages found)${NC}"
        return 1
    fi
    
    return 0 # Dependencies appear to be installed
}

# Function to install dependencies
install_dependencies() {
    log_message "${YELLOW}Installing dependencies...${NC}"
    
    # Check if package-lock.json exists - if so, use npm ci for faster, more reliable install
    if [ -f "package-lock.json" ]; then
        log_message "${BLUE}Found package-lock.json, using 'npm ci' for consistent install${NC}"
        npm ci
    else
        npm install
    fi
    
    local result=$?
    
    if [ $result -ne 0 ]; then
        log_message "${RED}Dependency installation failed with error code $result.${NC}"
        exit 1
    fi
    
    log_message "${GREEN}Dependencies installed successfully.${NC}"
}

# Function to start the development server
start_dev_server() {
    log_message "${BLUE}Starting development server...${NC}"
    npm run dev
}

# Main execution starts here
log_message "${BLUE}Client starter script running...${NC}"

# Check if we're in the correct directory
if [ ! -f "package.json" ]; then
    # Try to find client directory
    CLIENT_DIR="one-day-poc-client"
    
    if [ -d "$CLIENT_DIR" ] && [ -f "$CLIENT_DIR/package.json" ]; then
        log_message "${YELLOW}Changing to client directory: $CLIENT_DIR${NC}"
        cd "$CLIENT_DIR"
    else
        log_message "${RED}Error: package.json not found. Please run this script from the client directory.${NC}"
        exit 1
    fi
fi

# Check current directory name
DIR_NAME=$(basename "$(pwd)")
log_message "Current directory: $DIR_NAME"

# Check if dependencies are installed
if check_dependencies; then
    log_message "${GREEN}Dependencies already installed.${NC}"
else
    log_message "${YELLOW}Dependencies not found or incomplete.${NC}"
    install_dependencies
fi

# Check if node_modules/.bin exists - this is a good indicator of a complete installation
if [ ! -d "node_modules/.bin" ]; then
    log_message "${RED}Warning: node_modules/.bin directory missing. Installation may be incomplete.${NC}"
    log_message "${YELLOW}Attempting to fix by reinstalling dependencies...${NC}"
    install_dependencies
fi

# Start the development server
start_dev_server

# Exit with the status code of the npm run dev command
exit $?
