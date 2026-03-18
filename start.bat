@echo off
echo ==================================
echo  Starting Report Generation Tool
echo ==================================

echo --^> Starting Backend...
cd backend
start "Report Backend" cmd /c "mvn spring-boot:run -Dspring-boot.run.profiles=h2"
cd ..

echo --^> Starting Frontend...
cd frontend
start "Report Frontend" cmd /c "npm run dev"
cd ..

echo ==================================
echo  Both applications are starting in separate windows.
echo  You can close those windows manually or run stop.bat
echo ==================================
