#!/usr/bin/env bash

set -euo pipefail

if [[ ! -f .env.prod ]]; then
    echo ".env.prod is missing. Run ./deploy/create-prod-env.sh first."
    exit 1
fi

docker compose --env-file .env.prod -f compose.prod.yaml up -d --build
docker compose --env-file .env.prod -f compose.prod.yaml ps
