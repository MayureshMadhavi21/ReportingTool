#!/bin/bash

echo "=================================="
echo " Building Report Generation Tool"
echo "=================================="

echo "--> Building Report Service (Spring Boot)..."
cd report-service
mvn clean install -DskipTests
cd ..

echo "--> Building Connector & Query Service (Spring Boot)..."
cd connector-query-service
mvn clean install -DskipTests
cd ..

echo "--> Building Frontend (React/Vite)..."
cd frontend
npm install
npm run build
cd ..

echo "=================================="
echo " Build Complete!"
echo "=================================="
