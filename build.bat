@echo off
echo ==================================
echo  Building Report Generation Tool
echo ==================================

echo --^> Building Report Service (Spring Boot)...
cd report-service
call mvn clean install -DskipTests
cd ..

echo --^> Building Connector ^& Query Service (Spring Boot)...
cd connector-query-service
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
