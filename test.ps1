# PowerShell script to run end-to-end tests for a project
# This script will:
# 1. Call proofit.ps1 in the background to set up testing environment
# 2. Wait for services to be fully running
# 3. Run the Playwright tests from the one-day-poc-e2e-testing directory
# 4. Report results and clean up

# Display commands as they execute (similar to bash set -x)
Set-PSDebug -Trace 1

Write-Host "Starting E2E testing process..."

# Step 1: Call proofit.ps1 in the background
Write-Host "Setting up testing environment..."

# Start proofit.ps1 in the background
$proofitJob = Start-Job -ScriptBlock { 
    & "$using:PWD\proofit.ps1" 
}

# Give proofit.ps1 a moment to start initializing
Start-Sleep -Seconds 3

Write-Host "Proofit started in the background (Job ID: $($proofitJob.Id))"

# Step 2: Wait for services to be fully running
Write-Host "Waiting for services to start..."

# Function to check if services are running
function Test-ServicesRunning {
    # Check if all required services are running by testing if their ports are open
    # We need ports 5173, 7000, and 8000 to be available
    
    $allPortsOpen = $true
    $waitingPorts = @()
    
    # Check port 5173 (e.g., Vite dev server)
    $port5173 = Test-NetConnection -ComputerName localhost -Port 5173 -WarningAction SilentlyContinue
    if (-not $port5173.TcpTestSucceeded) {
        $allPortsOpen = $false
        $waitingPorts += "5173"
    }
    
    # Check port 7000 (e.g., API service)
    $port7000 = Test-NetConnection -ComputerName localhost -Port 7000 -WarningAction SilentlyContinue
    if (-not $port7000.TcpTestSucceeded) {
        $allPortsOpen = $false
        $waitingPorts += "7000"
    }
    
    # Check port 8000 (e.g., database or another service)
    $port8000 = Test-NetConnection -ComputerName localhost -Port 8000 -WarningAction SilentlyContinue
    if (-not $port8000.TcpTestSucceeded) {
        $allPortsOpen = $false
        $waitingPorts += "8000"
    }
    
    # If any ports are not open, display which ones we're waiting for
    if (-not $allPortsOpen) {
        Write-Host "Waiting for services:"
        foreach ($port in $waitingPorts) {
            Write-Host "  - Port $port not ready"
        }
    }
    
    return $allPortsOpen
}

# Try up to 30 times (5 minutes total) with 10-second intervals
$maxRetries = 30
$retryInterval = 10
$retryCount = 0

while (-not (Test-ServicesRunning)) {
    $retryCount++
    
    if ($retryCount -ge $maxRetries) {
        Write-Host "Error: Services failed to start after $($maxRetries * $retryInterval) seconds."
        # Clean up the background job
        Remove-Job -Job $proofitJob -Force
        exit 1
    }
    
    Write-Host "Services not ready yet. Waiting $retryInterval seconds... (Attempt $retryCount/$maxRetries)"
    Start-Sleep -Seconds $retryInterval
}

Write-Host "All services are now running!"

# Step 3: Run Playwright tests
Write-Host "Running Playwright tests..."

# Set any environment variables needed for tests
$env:NODE_ENV = "test"

# Change to the e2e testing directory
Push-Location -Path "one-day-poc-e2e-testing"

# Run the tests
npx playwright test

# Capture the exit code of the Playwright test command
$testExitCode = $LASTEXITCODE

# Return to the original directory
Pop-Location

# Step 4: Report results
if ($testExitCode -eq 0) {
    Write-Host "✅ All tests passed successfully!"
} else {
    Write-Host "❌ Tests failed with exit code $testExitCode"
}

# Clean up resources
Write-Host "Cleaning up resources..."

# Stop the background job running proofit.ps1
Write-Host "Stopping proofit job..."
Stop-Job -Job $proofitJob
Remove-Job -Job $proofitJob

# Return the test exit code as this script's exit code
exit $testExitCode
