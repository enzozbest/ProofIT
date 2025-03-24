# dev-manager.ps1 - A PowerShell script to manage development processes
# This script starts the client development server and server processes,
# captures their output, and ensures clean termination when interrupted.

# Color settings for better readability
$Green = "Green"
$Blue = "Cyan"
$Yellow = "Yellow"
$Red = "Red"

# Base directory and log paths
$BaseDir = Get-Location
$LogDir = Join-Path -Path $BaseDir -ChildPath "logs"
$ClientLog = Join-Path -Path $LogDir -ChildPath "client-dev.log"
$ServerLog = Join-Path -Path $LogDir -ChildPath "server-dev.log"

# Store process objects for later termination
$ClientProcess = $null
$ServerProcess = $null

# Store job objects for log monitoring
$ClientLogJob = $null
$ServerLogJob = $null

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

# Function to monitor a log file and display new content
function Monitor-LogFile {
    param (
        [string]$LogFile,
        [string]$Prefix,
        [string]$PrefixColor
    )
    
    # Create file if it doesn't exist
    if (-not (Test-Path $LogFile)) {
        New-Item -Path $LogFile -ItemType File -Force | Out-Null
    }
    
    # Start a job to monitor the log file
    $job = Start-Job -ScriptBlock {
        param($logFile, $prefix, $prefixColor)
        
        # Get initial file size
        $lastPosition = 0
        if (Test-Path $logFile) {
            $lastPosition = (Get-Item $logFile).Length
        }
        
        while ($true) {
            Start-Sleep -Milliseconds 100
            
            if (Test-Path $logFile) {
                $file = Get-Item $logFile
                
                if ($file.Length -gt $lastPosition) {
                    $reader = [System.IO.File]::OpenText($logFile)
                    $reader.BaseStream.Position = $lastPosition
                    
                    while ($line = $reader.ReadLine()) {
                        # Output through a custom object to preserve color information
                        [PSCustomObject]@{
                            Prefix = $prefix
                            PrefixColor = $prefixColor
                            Content = $line
                        }
                    }
                    
                    $lastPosition = $reader.BaseStream.Position
                    $reader.Close()
                }
            }
        }
    } -ArgumentList $LogFile, $Prefix, $PrefixColor
    
    # Return the job object for later management
    return $job
}

# Function to receive and display log monitor output
function Receive-LogOutput {
    param (
        [System.Management.Automation.Job]$Job
    )
    
    $output = Receive-Job -Job $Job -Keep
    foreach ($line in $output) {
        Write-Host $line.Prefix -ForegroundColor $line.PrefixColor -NoNewline
        Write-Host " $($line.Content)"
    }
}

# Function to cleanup on exit
function Cleanup {
    Log-Message "Received termination signal. Cleaning up..." -Color $Red
    
    # Stop client process
    if ($null -ne $ClientProcess -and -not $ClientProcess.HasExited) {
        Log-Message "Stopping client development server..." -Color $Yellow
        try {
            Stop-Process -Id $ClientProcess.Id -Force -ErrorAction SilentlyContinue
        } catch {
            Log-Message "Error stopping client process: $_" -Color $Red
        }
    }
    
    # Stop server process and its children
    if ($null -ne $ServerProcess -and -not $ServerProcess.HasExited) {
        Log-Message "Stopping server processes..." -Color $Yellow
        
        try {
            # First try to send Ctrl+C to the process (similar to SIGINT)
            $signature = @"
            [DllImport("kernel32.dll")]
            public static extern bool GenerateConsoleCtrlEvent(uint dwCtrlEvent, uint dwProcessGroupId);
"@
            $generateConsoleCtrlEvent = Add-Type -MemberDefinition $signature -Name GenerateConsoleCtrlEvent -Namespace Win32Functions -PassThru
            
            # 0 is the CTRL_C_EVENT code
            $generateConsoleCtrlEvent::GenerateConsoleCtrlEvent(0, $ServerProcess.Id) | Out-Null
            
            # Give it a moment to handle its own cleanup
            Start-Sleep -Seconds 2
            
            # If still running, force terminate
            if (-not $ServerProcess.HasExited) {
                Log-Message "Server did not respond to Ctrl+C. Force terminating..." -Color $Red
                Stop-Process -Id $ServerProcess.Id -Force -ErrorAction SilentlyContinue
            }
            
            # Find and kill any child processes
            Get-WmiObject Win32_Process | Where-Object { $_.ParentProcessId -eq $ServerProcess.Id } | ForEach-Object {
                Log-Message "Stopping child process with PID: $($_.ProcessId)" -Color $Yellow
                Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
            }
        } catch {
            Log-Message "Error stopping server process: $_" -Color $Red
        }
    }
    
    # Stop log monitoring jobs
    if ($null -ne $ClientLogJob) {
        Stop-Job -Job $ClientLogJob -ErrorAction SilentlyContinue
        Remove-Job -Job $ClientLogJob -Force -ErrorAction SilentlyContinue
    }
    
    if ($null -ne $ServerLogJob) {
        Stop-Job -Job $ServerLogJob -ErrorAction SilentlyContinue
        Remove-Job -Job $ServerLogJob -Force -ErrorAction SilentlyContinue
    }
    
    Log-Message "All processes have been terminated." -Color $Green
    Log-Message "Exiting dev-manager script." -Color $Green
    
    # Remove the event handler to avoid duplicate handlers if script is run again
    Unregister-Event -SourceIdentifier "CtrlC" -ErrorAction SilentlyContinue
    
    exit
}

# Create a handler for Ctrl+C events
try {
    # Register event to capture Ctrl+C
    $null = Register-ObjectEvent -InputObject ([System.Console]) -EventName CancelKeyPress -Action {
        Cleanup
    } -SourceIdentifier "CtrlC"
} catch {
    Log-Message "Error setting up Ctrl+C handler: $_" -Color $Red
    Log-Message "Cleanup on Ctrl+C may not work properly." -Color $Red
}

# Main execution starts here
Log-Message "Starting development environment..." -Color $Blue

# Display the current directory
Log-Message "Current directory: $BaseDir"

# Create log directory if it doesn't exist
if (-not (Test-Path $LogDir)) {
    New-Item -Path $LogDir -ItemType Directory | Out-Null
    Log-Message "Created log directory: $LogDir"
}

# Check if the client directory exists
$ClientDir = Join-Path -Path $BaseDir -ChildPath "one-day-poc-client"
if (-not (Test-Path $ClientDir)) {
    Log-Message "Error: 'one-day-poc-client' directory not found!" -Color $Red
    Log-Message "Please make sure you're running this script from the correct location." -Color $Red
    exit 1
}

# Start the client development server
Log-Message "Starting client development server..." -Color $Yellow
try {
    # Start npm in a new process and redirect output to log file
    $ClientStartInfo = New-Object System.Diagnostics.ProcessStartInfo
    $ClientStartInfo.FileName = "cmd.exe"
    $ClientStartInfo.Arguments = "/c npm run dev"
    $ClientStartInfo.WorkingDirectory = $ClientDir
    $ClientStartInfo.RedirectStandardOutput = $true
    $ClientStartInfo.RedirectStandardError = $true
    $ClientStartInfo.UseShellExecute = $false
    $ClientStartInfo.CreateNoWindow = $true
    
    $ClientProcess = New-Object System.Diagnostics.Process
    $ClientProcess.StartInfo = $ClientStartInfo
    $ClientProcess.EnableRaisingEvents = $true
    
    # Set up handlers for capturing output and writing to log file
    $ClientOutWriter = [System.IO.StreamWriter]::new($ClientLog)
    $ClientProcess.OutputDataReceived += {
        param($sender, $e)
        if ($null -ne $e.Data) {
            $ClientOutWriter.WriteLine($e.Data)
            $ClientOutWriter.Flush()
        }
    }
    $ClientProcess.ErrorDataReceived += {
        param($sender, $e)
        if ($null -ne $e.Data) {
            $ClientOutWriter.WriteLine("ERROR: $($e.Data)")
            $ClientOutWriter.Flush()
        }
    }
    
    # Start the process and begin capturing output
    $ClientProcess.Start() | Out-Null
    $ClientProcess.BeginOutputReadLine()
    $ClientProcess.BeginErrorReadLine()
    
    # Store a reference to the writer for cleanup
    $global:ClientOutWriter = $ClientOutWriter
    
    Log-Message "Client development server started with PID: $($ClientProcess.Id)"
} catch {
    Log-Message "Error starting client development server: $_" -Color $Red
    Cleanup
    exit 1
}

# Start the server using start.sh
Log-Message "Starting server using start.sh..." -Color $Yellow
$ServerDir = Join-Path -Path $BaseDir -ChildPath "one-day-poc-server"
$ServerScript = Join-Path -Path $ServerDir -ChildPath "start.sh"

if ((Test-Path $ServerDir) -and (Test-Path $ServerScript)) {
    try {
        # Start the server in a new process
        $ServerStartInfo = New-Object System.Diagnostics.ProcessStartInfo
        $ServerStartInfo.FileName = "cmd.exe"
        $ServerStartInfo.Arguments = "/c bash start.sh"
        $ServerStartInfo.WorkingDirectory = $ServerDir
        $ServerStartInfo.RedirectStandardOutput = $true
        $ServerStartInfo.RedirectStandardError = $true
        $ServerStartInfo.UseShellExecute = $false
        $ServerStartInfo.CreateNoWindow = $true
        
        $ServerProcess = New-Object System.Diagnostics.Process
        $ServerProcess.StartInfo = $ServerStartInfo
        $ServerProcess.EnableRaisingEvents = $true
        
        # Set up handlers for capturing output and writing to log file
        $ServerOutWriter = [System.IO.StreamWriter]::new($ServerLog)
        $ServerProcess.OutputDataReceived += {
            param($sender, $e)
            if ($null -ne $e.Data) {
                $ServerOutWriter.WriteLine($e.Data)
                $ServerOutWriter.Flush()
            }
        }
        $ServerProcess.ErrorDataReceived += {
            param($sender, $e)
            if ($null -ne $e.Data) {
                $ServerOutWriter.WriteLine("ERROR: $($e.Data)")
                $ServerOutWriter.Flush()
            }
        }
        
        # Start the process and begin capturing output
        $ServerProcess.Start() | Out-Null
        $ServerProcess.BeginOutputReadLine()
        $ServerProcess.BeginErrorReadLine()
        
        # Store a reference to the writer for cleanup
        $global:ServerOutWriter = $ServerOutWriter
        
        Log-Message "Server started with PID: $($ServerProcess.Id)"
    } catch {
        Log-Message "Error starting server: $_" -Color $Red
    }
} else {
    Log-Message "Warning: Could not find one-day-poc-server/start.sh" -Color $Red
    Log-Message "Continuing without starting server. Please check the project structure." -Color $Red
}

# Add a small delay to ensure log files exist
Start-Sleep -Seconds 1

# Start monitoring the log files
Log-Message "Monitoring client output (from $ClientLog):" -Color $Blue
$ClientLogJob = Monitor-LogFile -LogFile $ClientLog -Prefix "[CLIENT]" -PrefixColor $Blue

if ($null -ne $ServerProcess) {
    Log-Message "Monitoring server output (from $ServerLog):" -Color $Yellow
    $ServerLogJob = Monitor-LogFile -LogFile $ServerLog -Prefix "[SERVER]" -PrefixColor $Yellow
}

# Display process information
Log-Message "Client development server running with PID: $($ClientProcess.Id)" -Color $Green
if ($null -ne $ServerProcess) {
    Log-Message "Server running with PID: $($ServerProcess.Id)" -Color $Green
}

Log-Message "All processes started successfully!" -Color $Green
Log-Message "Press Ctrl+C to stop all processes and exit." -Color $Green

# Keep the script running and display log outputs
try {
    while ($true) {
        Start-Sleep -Milliseconds 100
        
        # Display log outputs
        if ($null -ne $ClientLogJob) {
            Receive-LogOutput -Job $ClientLogJob
        }
        
        if ($null -ne $ServerLogJob) {
            Receive-LogOutput -Job $ServerLogJob
        }
        
        # Check if client process is still running
        if ($null -ne $ClientProcess -and $ClientProcess.HasExited) {
            Log-Message "Client development server has stopped unexpectedly." -Color $Red
            Log-Message "Checking the last few lines of the log:" -Color $Red
            Get-Content -Path $ClientLog -Tail 10
            Cleanup
            break
        }
        
        # Check if server process is still running (if it was started)
        if ($null -ne $ServerProcess -and $ServerProcess.HasExited) {
            Log-Message "Server has stopped unexpectedly." -Color $Red
            Log-Message "Checking the last few lines of the log:" -Color $Red
            Get-Content -Path $ServerLog -Tail 10
            Cleanup
            break
        }
    }
} finally {
    # Make sure to clean up even if we exit the loop in an unexpected way
    Cleanup
}
