#!/bin/bash

echo "=================================="
echo " Stopping Report Generation Tool"
echo "=================================="

echo "--> Stopping Report Service..."
if [ -f "report-service/report-service.pid" ]; then
    PID=$(cat report-service/report-service.pid)
    echo "Killing Report Service (PID: $PID)..."
    kill $PID
    rm report-service/report-service.pid
else
    echo "Report Service PID not found. Is it running?"
fi

echo "--> Stopping Connector & Query Service..."
if [ -f "connector-query-service/connector-query-service.pid" ]; then
    PID=$(cat connector-query-service/connector-query-service.pid)
    echo "Killing Connector & Query Service (PID: $PID)..."
    kill $PID
    rm connector-query-service/connector-query-service.pid
else
    echo "Connector & Query Service PID not found. Is it running?"
fi

echo "--> Stopping Frontend..."
if [ -f "frontend/frontend.pid" ]; then
    FRONTEND_PID=$(cat frontend/frontend.pid)
    echo "Killing Frontend (PID: $FRONTEND_PID)..."
    kill $FRONTEND_PID
    rm frontend/frontend.pid
else
    echo "Frontend PID not found. Is it running?"
fi

echo "=================================="
echo " Stopped."
echo "=================================="
