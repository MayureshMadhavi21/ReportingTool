#!/bin/bash

echo "=================================="
echo " Stopping Report Generation Tool"
echo "=================================="

if [ -f "backend/backend.pid" ]; then
    BACKEND_PID=$(cat backend/backend.pid)
    echo "Killing Backend (PID: $BACKEND_PID)..."
    kill $BACKEND_PID
    rm backend/backend.pid
else
    echo "Backend PID not found. Is it running?"
fi

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
