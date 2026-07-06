@echo off
chcp 65001 >nul
cd /d "%~dp0.."

if exist ".venv\Scripts\python.exe" (
    .venv\Scripts\python.exe scripts\generate_chen_er_diagram.py
) else (
    python scripts\generate_chen_er_diagram.py
)

pause
