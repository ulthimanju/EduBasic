#!/bin/bash

echo "Starting EduBasic Application..."
echo ""

echo "Starting Backend (Spring Boot)..."
cd backend
./mvnw spring-boot:run &
BACKEND_PID=$!
cd ..

sleep 5

echo "Starting Frontend (React + Vite)..."
cd frontend
npm run dev &
FRONTEND_PID=$!
cd ..

echo ""
echo "Both servers are starting..."
echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:5173"
echo ""
echo "Press Ctrl+C to stop both servers"

# Wait for Ctrl+C
trap "kill $BACKEND_PID $FRONTEND_PID; exit" INT
wait
