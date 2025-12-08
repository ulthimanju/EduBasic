@echo off
echo Starting EduBas Application...
echo.

echo Starting Backend (Spring Boot)...
start "EduBas Backend" cmd /k "cd backend && mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak >nul

echo Starting Frontend (React + Vite)...
start "EduBas Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo Both servers are starting...
echo Backend: http://localhost:8080
echo Frontend: http://localhost:5173
echo.
pause
