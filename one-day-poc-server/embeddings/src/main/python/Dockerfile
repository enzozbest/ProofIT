FROM python:3.10-slim

# Install OpenJDK 21
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    ca-certificates \
    gnupg \
    build-essential \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Add Adoptium repository (provides OpenJDK packages)
RUN mkdir -p /etc/apt/keyrings && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /etc/apt/keyrings/adoptium.asc && \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list

# Install OpenJDK 21
RUN apt-get update && apt-get install -y --no-install-recommends \
    temurin-21-jdk \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64

# Create and set working directory
WORKDIR /app

# Copy requirements first to leverage Docker cache
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Set Python path
ENV PYTHONPATH=/app

# Command to run the application
RUN python3.10 -m information_retrieval
CMD ["python3.10", "information_retrieval/__main__.py"]
EXPOSE 7000
