@echo off
REM Script para ejecutar todos los microservicios en Windows

echo Iniciando microservicios...
echo.

echo Compilando inventory-service...
cd inventory-service && call gradlew.bat clean build -x test && cd ..
if %errorlevel% neq 0 ( echo Error en inventory-service. Abortando. & exit /b 1 )

echo Compilando order-service...
cd order-service && call gradlew.bat clean build -x test && cd ..
if %errorlevel% neq 0 ( echo Error en order-service. Abortando. & exit /b 1 )

echo Compilando payment-service...
cd payment-service && call gradlew.bat clean build -x test && cd ..
if %errorlevel% neq 0 ( echo Error en payment-service. Abortando. & exit /b 1 )

echo.
echo Compilacion exitosa
echo.

REM Crear directorio de logs si no existe
if not exist logs mkdir logs

echo Iniciando order-service en puerto 8081...
start "ORDER-SERVICE" cmd /c "cd order-service && gradlew.bat bootRun > ..\logs\order-service.log 2>&1"

timeout /t 5 /nobreak > nul

echo Iniciando payment-service en puerto 8082...
start "PAYMENT-SERVICE" cmd /c "cd payment-service && gradlew.bat bootRun > ..\logs\payment-service.log 2>&1"

timeout /t 3 /nobreak > nul

echo Iniciando inventory-service en puerto 8083...
start "INVENTORY-SERVICE" cmd /c "cd inventory-service && gradlew.bat bootRun > ..\logs\inventory-service.log 2>&1"

echo.
echo Todos los servicios iniciados en ventanas separadas
echo.
echo Logs disponibles en .\logs\
echo.
echo Cierra las ventanas para detener los servicios
echo.

pause
