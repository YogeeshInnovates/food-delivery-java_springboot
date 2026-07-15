#!/bin/bash

# Navigate to the directory where this script is located
cd "$(dirname "$0")"

# The .env file should be in the root directory (two levels up)
ENV_FILE="../../.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found at $ENV_FILE"
    echo "Please create the .env file in the root of the project before starting."
    exit 1
fi

echo "Found .env file. Starting production stack..."

# Start the docker-compose stack
# We run this from the deployment/docker directory, which is one level up from nginx/
cd ..
docker-compose -f docker-compose.prod.yml --env-file ../../.env up -d --build

echo "Production stack started!"
