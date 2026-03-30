@echo off
echo ==================================
echo  Starting Report Generation Tool
echo ==================================

echo --^> Starting Report Service...
cd report-service
start "Report Service" cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=h2"
cd ..

echo --^> Starting Connector ^& Query Service...
cd connector-query-service
start "Connector Service" cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=h2"
cd ..

echo --^> Starting Frontend...
cd frontend
start "Report Frontend" cmd /c "npm run dev"
cd ..

echo ==================================
echo  All 3 applications are starting in separate windows.
echo  You can close those windows manually or run stop.bat
echo ==================================
