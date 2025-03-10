@echo off
setlocal enabledelayedexpansion

:: Check if required parameters are provided
if "%~1"=="" (
    echo Usage: call_api.bat "<URL>" [GET|POST] ["JSON_PAYLOAD"]
    exit /b 1
)

:: Assign parameters
set "URL=%~1"
set "METHOD=%~2"
set "PAYLOAD=%~3"

:: Default method to GET if not provided
if "%METHOD%"=="" set "METHOD=GET"

:: Make API call using curl
if /I "%METHOD%"=="GET" (
    curl -X GET "%URL%" -H "Content-Type: application/json"
) else if /I "%METHOD%"=="POST" (
    if "%PAYLOAD%"=="" (
        echo JSON payload is required for POST requests.
        exit /b 1
    )
    curl -X POST "%URL%" -H "Content-Type: application/json" -d "%PAYLOAD%"
) else (
    echo Invalid method. Use GET or POST.
    exit /b 1
)

exit /b 0
