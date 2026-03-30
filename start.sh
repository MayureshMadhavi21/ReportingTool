#!/bin/bash

echo "=================================="
echo " Starting Report Generation Tool"
echo "=================================="

echo "--> Starting Report Service on port 8084..."
cd report-service
mvn spring-boot:run -Dspring-boot.run.profiles=h2 > report-service.log 2>&1 &
REPORT_PID=$!
echo $REPORT_PID > report-service.pid
echo "Report Service started with PID $REPORT_PID (Logs in report-service/report-service.log)"
cd ..

echo "--> Starting Connector & Query Service on port 8085..."
cd connector-query-service
mvn spring-boot:run -Dspring-boot.run.profiles=h2 > connector-query-service.log 2>&1 &
CONNECTOR_PID=$!
echo $CONNECTOR_PID > connector-query-service.pid
echo "Connector & Query Service started with PID $CONNECTOR_PID (Logs in connector-query-service/connector-query-service.log)"
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
