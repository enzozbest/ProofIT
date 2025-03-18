@echo off

REM Check if the virtual environment directory exists
if not exist "embeddings\src\main\python\venv" (
    echo Virtual environment for Python microservice not found. Creating venv and installing requirements...
    python3.10 -m venv embeddings\src\main\python\venv
    if not exist "embeddings\src\main\python\venv" (
        echo Virtual environment could not be created. Aborting!
        exit /b 1
    )
    call embeddings\src\main\python\venv\Scripts\activate.bat
    pip install -r embeddings\src\main\python\requirements.txt
) else (
    echo Virtual environment for Python microservices found. Activating...
    call embeddings\src\main\python\venv\Scripts\activate.bat
)

REM Run the Python module in the background and then run Gradle
start /B python3.10 embeddings\src\main\python\information_retrieval\__main__.py
call gradlew run
