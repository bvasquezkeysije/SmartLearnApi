@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Script para ejecutar migraciones SQL en orden manual.

set MIGRATIONS_DIR=%~dp0src\main\resources\db\migration
if not exist "%MIGRATIONS_DIR%" (
    echo ERROR: No se encontro la carpeta de migraciones: %MIGRATIONS_DIR%
    exit /b 1
)

set USER=%DB_USERNAME%
if "%USER%"=="" set USER=bvasquezkeysije

set DB=%DB_NAME%
if "%DB%"=="" set DB=smartlearn

set HOST=%DB_HOST%
if "%HOST%"=="" set HOST=localhost

set PORT=%DB_PORT%
if "%PORT%"=="" set PORT=5432

if "%DB_PASSWORD%"=="" (
    set PGPASSWORD=76636255ADK
) else (
    set PGPASSWORD=%DB_PASSWORD%
)

where psql >nul 2>&1
if errorlevel 1 (
    echo ERROR: psql no esta disponible en PATH.
    echo Instala PostgreSQL client o configura PATH/SMARTLEARN_PSQL_PATH.
    exit /b 1
)

cd /d "%MIGRATIONS_DIR%"

for /f "delims=" %%f in ('dir /b /on V*.sql') do (
    echo Ejecutando %%f ...
    psql -v ON_ERROR_STOP=1 -U %USER% -d %DB% -h %HOST% -p %PORT% -f "%%f"
    if errorlevel 1 (
        echo ERROR ejecutando %%f
        exit /b 1
    )
)

echo Migraciones completadas.
exit /b 0

