#!/bin/bash

# Check if URL is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <API_URL>"
    exit 1
fi

API_URL="$1"

# Make a GET request using curl
response=$(curl -s -o response.json -w "%{http_code}" "$API_URL")

# Read HTTP status code
http_code=$(cat response.json | jq -r 'if .status then .status else empty end')

# Check HTTP response code
if [[ "$response" -ge 200 && "$response" -lt 300 ]]; then
    echo "✅ API Call Successful:"
    cat response.json | jq .
elif [[ "$response" -ge 400 ]]; then
    echo "❌ API Call Failed with HTTP Code: $response"
    cat response.json | jq .
else
    echo "⚠️ Unexpected Response:"
    cat response.json
fi

# Clean up response file
rm -f response.json
