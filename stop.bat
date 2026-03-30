@echo off
echo ==================================
echo  Stopping Report Generation Tool
echo ==================================

echo --^> Killing Backend and Frontend windows...
taskkill /FI "WindowTitle eq Report Service*" /T /F
taskkill /FI "WindowTitle eq Connector Service*" /T /F
taskkill /FI "WindowTitle eq Report Frontend*" /T /F

echo ==================================
echo  Stopped.
echo ==================================
pause
