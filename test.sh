#!/bin/bash

# Script to run end-to-end tests for a project
# This script will:
# 1. Call proofit.sh to set up testing environment
# 2. Wait for services to be fully running
# 3. Run the Playwright tests
# 4. Report results

# Exit immediately if a command exits with a non-zero status
set -e

# Print commands before executing them (helpful for debugging)
set -x

echo "Starting E2E testing process..."

# Step 1: Call proofit.sh in the background
echo "Setting up testing environment..."
./proofit.sh &

# Store the PID of the background process
PROOFIT_PID=$!

# Give proofit.sh a moment to start initializing
sleep 3

echo "Proofit started in the background (PID: $PROOFIT_PID)"

# Step 2: Wait for services to be fully running
echo "Waiting for services to start..."

# Function to check if services are running
check_services_running() {
  # Check if all required services are running by testing if their ports are open
  # We need ports 5173, 7000, and 8000 to be available
  
  # Check port 5173 (e.g., Vite dev server)
  nc -z localhost 5173 &> /dev/null
  port_5173_status=$?
  
  # Check port 7000 (e.g., API service)
  nc -z localhost 7000 &> /dev/null
  port_7000_status=$?
  
  # Check port 8000 (e.g., database or another service)
  nc -z localhost 8000 &> /dev/null
  port_8000_status=$?
  
  # All ports must be available for services to be considered running
  if [ $port_5173_status -eq 0 ] && [ $port_7000_status -eq 0 ] && [ $port_8000_status -eq 0 ]; then
    return 0  # Success - all services are running
  else
    # Provide helpful debug information about which ports are not yet available
    echo "Waiting for services:"
    [ $port_5173_status -ne 0 ] && echo "  - Port 5173 not ready"
    [ $port_7000_status -ne 0 ] && echo "  - Port 7000 not ready"
    [ $port_8000_status -ne 0 ] && echo "  - Port 8000 not ready"
    return 1  # Failure - some services are not running yet
  fi
}

# Try up to 30 times (5 minutes total) with 10-second intervals
MAX_RETRIES=30
RETRY_INTERVAL=10
retry_count=0

while ! check_services_running; do
  retry_count=$((retry_count + 1))
  
  if [ $retry_count -ge $MAX_RETRIES ]; then
    echo "Error: Services failed to start after $((MAX_RETRIES * RETRY_INTERVAL)) seconds."
    exit 1
  fi
  
  echo "Services not ready yet. Waiting $RETRY_INTERVAL seconds... (Attempt $retry_count/$MAX_RETRIES)"
  sleep $RETRY_INTERVAL
done

echo "All services are now running!"

# Step 3: Run Playwright tests
echo "Running Playwright tests..."

# Set any environment variables needed for tests
export NODE_ENV=test

# Run the tests
cd one-day-poc-e2e-testing
npx playwright test

# Capture the exit code of the Playwright test command
test_exit_code=$?

# Step 4: Report results
if [ $test_exit_code -eq 0 ]; then
  echo "✅ All tests passed successfully!"
else
  echo "❌ Tests failed with exit code $test_exit_code"
fi

# Optional: Clean up any resources
# echo "Cleaning up resources..."
# Add cleanup commands here if needed
cd ..
# Return the test exit code as this script's exit code
exit $test_exit_code
