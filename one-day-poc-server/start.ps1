# Function to activate the virtual environment in the current session
function Activate-Venv
{
    $venvActivateScript = "embeddings\src\main\python\venv\Scripts\Activate.ps1"
    if (Test-Path $venvActivateScript)
    {
        Write-Host "Activating virtual environment..."
        . $venvActivateScript  # Dot-source to activate in the current session
    }
    else
    {
        Write-Host "Activation script not found: $venvActivateScript"
        exit 1
    }
}

$venvPath = "embeddings\src\main\python\venv"

# Check if the virtual environment exists; if not, create and install requirements.
if (-not (Test-Path $venvPath))
{
    Write-Host "Virtual environment not found. Creating venv and installing requirements..."
    python3.10 -m venv $venvPath

    if (-not (Test-Path $venvPath))
    {
        Write-Host "Virtual environment could not be created. Aborting!"
        exit 1
    }

    Activate-Venv

    Write-Host "Installing Python requirements..."
    pip install -r "embeddings\src\main\python\requirements.txt"
}
else
{
    Write-Host "Virtual environment found. Activating..."
    Activate-Venv
}

# Set PYTHONPATH so that the package 'information_retrieval' is found.
$env:PYTHONPATH = (Join-Path (Get-Location) "embeddings\src\main\python")


# Start the Python microservice; capture its process info
$pythonProc = Start-Process -FilePath "python3.10" `
    -ArgumentList "embeddings\src\main\python\information_retrieval\__main__.py" `
    -PassThru -NoNewWindow

# Start Gradle; ensure you call gradlew.bat if that's what you have
$gradleProc = Start-Process -FilePath "gradlew" `
    -ArgumentList "run" `
    -PassThru -NoNewWindow

# Register a handler for Ctrl+C (SIGINT) that will stop both processes
Register-EngineEvent -SourceIdentifier ConsoleCancelEvent -Action {
    Write-Host "Ctrl+C pressed. Stopping processes..."
    if ($pythonProc -and -not $pythonProc.HasExited)
    {
        Stop-Process -Id $pythonProc.Id -Force
    }
    if ($gradleProc -and -not $gradleProc.HasExited)
    {
        Stop-Process -Id $gradleProc.Id -Force
    }
    exit
} | Out-Null

# Wait for both processes to exit
Wait-Process -Id $pythonProc.Id, $gradleProc.Id
