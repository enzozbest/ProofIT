#!/bin/bash

# Check if the virtual environment directory exists
if [ ! -d "./embeddings/src/main/python/venv" ]; then
    echo "Virtual environment for Python microservice not found. Creating venv and installing requirements..."
    python3.10 -m venv ./embeddings/src/main/python/venv
    if [ ! -d "./embeddings/src/main/python/venv" ]; then
      echo "Virtual environment could not be created. Aborting!"
      exit 1
    fi
    source ./embeddings/src/main/python/venv/bin/activate
    pip install -r ./embeddings/src/main/python/requirements.txt
else
    echo "Virtual environment for Python microservicec found! Activating..."
    source ./embeddings/src/main/python/venv/bin/activate
fi

# Run the Python module in the background and then run Gradle
python3.10 ./embeddings/src/main/python/information_retrieval/__main__.py & ./gradlew run
