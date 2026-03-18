#!/bin/bash

echo "=================================="
echo " Starting Report Generation Tool"
echo "=================================="

echo "--> Starting Backend on port 8084..."
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2 > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid
echo "Backend started with PID $BACKEND_PID (Logs in backend/backend.log)"
cd ..

echo "--> Starting Frontend on port 5173..."
cd frontend
npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > frontend.pid
echo "Frontend started with PID $FRONTEND_PID (Logs in frontend/frontend.log)"
cd ..

echo "=================================="
echo " Applications are running in the background."
echo " Use ./stop.sh to terminate them."
echo "=================================="
