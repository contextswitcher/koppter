@echo off
rem Runs koppter from this checkout until it is quit.
rem The exit code 55 means that "Restart to update" was clicked: pull, build and start again.
rem Any other exit code ends the loop.
cd /d "%~dp0.."
:loop
rem Without a network, the application starts as it is.
call git pull --no-rebase || echo run-loop: not pulled, starting what is here
rem Tells the application which commit it runs, and that its exit with 55 starts it again.
for /f %%c in ('git rev-parse HEAD') do set KOPPTER_RUN_LOOP=%%c
call gradlew.bat :app:createDistributable || exit /b 1
app\build\compose\binaries\main\app\koppter\koppter.exe
if %errorlevel% equ 55 goto loop
exit /b %errorlevel%
