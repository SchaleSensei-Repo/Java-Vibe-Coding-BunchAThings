@echo off
REM Automatically download all JARs listed in libs.txt
mkdir libs 2>nul
for /f "tokens=*" %%A in (libs.txt) do (
    echo Downloading %%A...
    powershell -Command "Invoke-WebRequest -Uri %%A -OutFile libs\\%%~nxA"
)
