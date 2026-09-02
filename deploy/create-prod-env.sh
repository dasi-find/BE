#!/usr/bin/env bash

set -euo pipefail

env_file="${1:-.env.prod}"

if [[ -e "$env_file" ]]; then
    echo "$env_file already exists. Refusing to overwrite it."
    exit 1
fi

read -r -p "Gmail address: " mail_username
read -r -s -p "Google app password: " mail_password
echo
read -r -p "Private S3 bucket name: " s3_bucket

if [[ -z "$s3_bucket" ]]; then
    echo "S3 bucket name is required."
    exit 1
fi

mail_password="${mail_password// /}"
umask 077

{
    echo "DB_NAME=dasi_find"
    echo "DB_USERNAME=dasi_find"
    echo "DB_PASSWORD=$(openssl rand -hex 24)"
    echo "DB_ROOT_PASSWORD=$(openssl rand -hex 24)"
    echo "REDIS_PASSWORD=$(openssl rand -hex 24)"
    echo "JWT_SECRET=$(openssl rand -hex 48)"
    echo "MAIL_USERNAME=$mail_username"
    echo "MAIL_PASSWORD=$mail_password"
    echo "MAIL_FROM=$mail_username"
    echo "CORS_ALLOWED_ORIGINS=https://dasifind.vercel.app"
    echo "AWS_REGION=ap-northeast-2"
    echo "AWS_S3_BUCKET=$s3_bucket"
    echo "AI_ANALYSIS_BASE_URL="
    echo "AUTH_COOKIE_SAME_SITE=LAX"
} > "$env_file"

echo "Created $env_file with permissions 600."
