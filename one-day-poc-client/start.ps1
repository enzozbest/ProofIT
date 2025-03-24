# client-starter.ps1 - Script to start the client application
# This script checks if dependencies are installed before starting the client

# Color settings for better readability
$Green = "Green"
$Blue = "Cyan"
$Yellow = "Yellow"
$Red = "Red"

# Function to display messages with timestamp
function Log-Message {
    param (
        [string]$Message,
        [string]$Color = "White"
    )
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp]" -ForegroundColor $Green -NoNewline
    Write-Host " $Message" -ForegroundColor $Color
}

# Function to check if node_modules exists and has content
function Check-Dependencies {
    # Check if node_modules directory exists
    if (-not (Test-Path "node_modules")) {
        return $false # Dependencies not installed
    }

    # Check if node_modules has content (not empty)
    $nodeModulesContent = Get-ChildItem -Path "node_modules" -ErrorAction SilentlyContinue
    if ($null -eq $nodeModulesContent -or $nodeModulesContent.Count -eq 0) {
        return $false # Dependencies not installed or directory empty
    }

    # Check if package.json exists
    if (-not (Test-Path "package.json")) {
        Log-Message "Error: package.json not found. Are you in the correct directory?" -Color $Red
        exit 1
    }

    # Get rough count of installed modules (just top-level directories)
    $installedCount = (Get-ChildItem -Path "node_modules" -Directory).Count
    
    # If the number of installed packages seems significantly lower than expected,
    # assume the installation is incomplete
    if ($installedCount -lt 5) {
        Log-Message "Detected potentially incomplete installation (only $installedCount packages found)" -Color $Yellow
        return $false
    }
    
    return $true # Dependencies appear to be installed
}

# Function to install dependencies
function Install-Dependencies {
    Log-Message "Installing dependencies..." -Color $Yellow
    
    # Check if package-lock.json exists - if so, use npm ci for faster, more reliable install
    if (Test-Path "package-lock.json") {
        Log-Message "Found package-lock.json, using 'npm ci' for consistent install" -Color $Blue
        $process = Start-Process -FilePath "npm" -ArgumentList "ci" -NoNewWindow -PassThru -Wait
        $exitCode = $process.ExitCode
    } else {
        $process = Start-Process -FilePath "npm" -ArgumentList "install" -NoNewWindow -PassThru -Wait
        $exitCode = $process.ExitCode
    }
    
    if ($exitCode -ne 0) {
        Log-Message "Dependency installation failed with error code $exitCode." -Color $Red
        exit 1
    }
    
    Log-Message "Dependencies installed successfully." -Color $Green
}

# Function to start the development server
function Start-DevServer {
    Log-Message "Starting development server..." -Color $Blue
    
    # Use npm run dev to start the dev server
    # We use Start-Process without -Wait because we want to keep the process running
    npm run dev
    
    # The exit code will be the exit code of the last npm run dev command
    return $LASTEXITCODE
}

# Main execution starts here
Log-Message "Client starter script running..." -Color $Blue

# Check if we're in the correct directory
if (-not (Test-Path "package.json")) {
    # Try to find client directory
    $clientDir = "one-day-poc-client"
    
    if ((Test-Path $clientDir) -and (Test-Path "$clientDir\package.json")) {
        Log-Message "Changing to client directory: $clientDir" -Color $Yellow
        Set-Location -Path $clientDir
    } else {
        Log-Message "Error: package.json not found. Please run this script from the client directory." -Color $Red
        exit 1
    }
}

# Check current directory name
$dirName = Split-Path -Path (Get-Location) -Leaf
Log-Message "Current directory: $dirName"

# Check if dependencies are installed
if (Check-Dependencies) {
    Log-Message "Dependencies already installed." -Color $Green
} else {
    Log-Message "Dependencies not found or incomplete." -Color $Yellow
    Install-Dependencies
}

# Check if node_modules\.bin exists - this is a good indicator of a complete installation
if (-not (Test-Path "node_modules\.bin")) {
    Log-Message "Warning: node_modules\.bin directory missing. Installation may be incomplete." -Color $Red
    Log-Message "Attempting to fix by reinstalling dependencies..." -Color $Yellow
    Install-Dependencies
}

# Start the development server
$exitCode = Start-DevServer

# Exit with the status code of the npm run dev command
exit $exitCode
