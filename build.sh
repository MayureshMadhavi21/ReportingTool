#!/bin/bash

echo "=================================="
echo " Building Report Generation Tool"
echo "=================================="

echo "--> Building Backend (Spring Boot)..."
cd backend
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
