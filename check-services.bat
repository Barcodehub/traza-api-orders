@echo off
REM Script de verificación de servicios

echo Verificando servicios...
echo.

set ALL_OK=1

REM Verificar order-service
echo Verificando order-service en puerto 8081...
curl -s -o nul -w "%%{http_code}" http://localhost:8081 > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] order-service
) else (
    echo [FAILED] order-service
    set ALL_OK=0
)

REM Verificar payment-service
echo Verificando payment-service en puerto 8082...
curl -s -o nul -w "%%{http_code}" http://localhost:8082 > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] payment-service
) else (
    echo [FAILED] payment-service
    set ALL_OK=0
)

REM Verificar inventory-service
echo Verificando inventory-service en puerto 8083...
curl -s -o nul -w "%%{http_code}" http://localhost:8083 > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] inventory-service
) else (
    echo [FAILED] inventory-service
    set ALL_OK=0
)

echo.

if %ALL_OK% equ 1 (
    echo Todos los servicios estan funcionando correctamente
    echo.
    echo Puedes probar creando una orden:
    echo.
    echo curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"userId\": \"user-123\", \"total\": 99.99}"
    echo.
) else (
    echo Algunos servicios no estan disponibles
    echo.
    echo Asegurate de que todos los servicios esten corriendo
    echo.
)

pause
