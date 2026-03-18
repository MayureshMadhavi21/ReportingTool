@echo off
echo ==================================
echo  Building Report Generation Tool
echo ==================================

echo --^> Building Backend (Spring Boot)...
cd backend
call mvn clean install -DskipTests
cd ..

echo --^> Building Frontend (React/Vite)...
cd frontend
call npm install
call npm run build
cd ..

echo ==================================
echo  Build Complete!
echo ==================================
pause
