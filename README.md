# Microservicios con Saga Orquestada - Java Spring Boot

Sistema de microservicios diseñado para implementar observabilidad y trazabilidad mediante patrón **Saga Orquestada**.

## 📋 Arquitectura

### Microservicios

1. **order-service** (Puerto 8081)
   - Orquestador de la saga
   - Persiste órdenes en H2
   - Coordina el flujo completo
   - Ejecuta compensaciones en caso de fallo

2. **payment-service** (Puerto 8082)
   - Procesa pagos
   - Simula fallos aleatorios (30%)
   - Stateless

3. **inventory-service** (Puerto 8083)
   - Reserva inventario
   - Simula fallos aleatorios (20%)
   - Stateless

### Comunicación

- **REST** entre servicios (RestTemplate)
- Headers propagados:
  - `X-Saga-Id`: ID único de la saga
  - `X-User-Id`: ID del usuario

### Flujo Saga

```
POST /orders
    ↓
1. Crear orden (PENDING)
    ↓
2. Llamar payment-service → SUCCESS
    ↓
3. Llamar inventory-service → SUCCESS
    ↓
4. Orden → CONFIRMED
```

**Flujo con error:**

```
POST /orders
    ↓
1. Crear orden (PENDING)
    ↓
2. Llamar payment-service → SUCCESS
    ↓
3. Llamar inventory-service → FAILED
    ↓
4. COMPENSACIÓN: refund payment
    ↓
5. Orden → CANCELLED
```

---

## 🚀 Cómo Ejecutar

### Requisitos

- Java 21
- Gradle 8.x

### Método 1: Ejecutar con Gradle (Recomendado)

Abrir **3 terminales** y ejecutar:

**Terminal 1 - Order Service:**
```bash
./gradlew :order-service:bootRun
```

**Terminal 2 - Payment Service:**
```bash
./gradlew :payment-service:bootRun
```

**Terminal 3 - Inventory Service:**
```bash
./gradlew :inventory-service:bootRun
```

### Método 2: Compilar JARs

```bash
# Compilar todos los servicios
./gradlew clean build

# Ejecutar cada servicio
java -jar order-service/build/libs/order-service-0.0.1-SNAPSHOT.jar
java -jar payment-service/build/libs/payment-service-0.0.1-SNAPSHOT.jar
java -jar inventory-service/build/libs/inventory-service-0.0.1-SNAPSHOT.jar
```

### Verificar que están corriendo

```bash
# Order Service
curl http://localhost:8081/actuator/health

# Payment Service
curl http://localhost:8082/actuator/health

# Inventory Service
curl http://localhost:8083/actuator/health
```

Si no tienes actuator, verifica los logs o prueba directamente los endpoints.

---

## 📡 API Endpoints

### Order Service (8081)

#### Crear Orden (Kafka)

```bash
# Envío exitoso
curl -X POST http://localhost:8081/api/kafka/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "total": 99.99
  }'

# Envío con fallo intencional (simulado en el consumidor)
curl -X POST http://localhost:8081/api/kafka/send-fail \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-fail",
    "total": 50.00
  }'
```

#### Crear Orden (REST Saga)

```bash
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "total": 99.99
  }'
```

**Respuesta exitosa:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-123",
  "status": "CONFIRMED",
  "total": 99.99,
  "sagaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Respuesta con fallo:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-123",
  "status": "CANCELLED",
  "total": 99.99,
  "sagaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### Consultar Orden

```bash
curl http://localhost:8081/orders/{orderId}
```

Ejemplo:
```bash
curl http://localhost:8081/orders/550e8400-e29b-41d4-a716-446655440000
```

---

### Payment Service (8082)

#### Forzar fallo en próximo request

```bash
# Activar fallo forzado
curl -X POST "http://localhost:8082/test/failure?enable=true"

# Desactivar fallo forzado (vuelve a modo aleatorio 30%)
curl -X POST "http://localhost:8082/test/failure?enable=false"
```

---

### Inventory Service (8083)

#### Forzar fallo en próximo request

```bash
# Activar fallo forzado
curl -X POST "http://localhost:8083/test/failure?enable=true"

# Desactivar fallo forzado (vuelve a modo aleatorio 20%)
curl -X POST "http://localhost:8083/test/failure?enable=false"
```

---

## 🧪 Escenarios de Prueba

### 1. Flujo exitoso completo

```bash
# Crear orden (puede requerir varios intentos hasta que tenga éxito)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "total": 150.00
  }'

# Verificar orden creada
curl http://localhost:8081/orders/{orderId}
```

Revisar logs para ver el flujo completo con `X-Saga-Id`.

---

### 2. Fallo en Payment Service

```bash
# Forzar fallo en payment
curl -X POST "http://localhost:8082/test/failure?enable=true"

# Crear orden (fallará en payment)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-456",
    "total": 200.00
  }'

# Verificar que orden está CANCELLED
curl http://localhost:8081/orders/{orderId}

# Restaurar payment service
curl -X POST "http://localhost:8082/test/failure?enable=false"
```

**Logs esperados:**
```
[SAGA:xxx] Starting order creation
[SAGA:xxx] Order created with status PENDING
[SAGA:xxx] Calling payment-service
[SAGA:xxx] Payment FAILED (simulated)
[SAGA:xxx] Order CANCELLED
```

---

### 3. Fallo en Inventory Service (con compensación de payment)

```bash
# Forzar fallo en inventory
curl -X POST "http://localhost:8083/test/failure?enable=true"

# Crear orden (fallará en inventory, compensará payment)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-789",
    "total": 300.00
  }'

# Verificar que orden está CANCELLED
curl http://localhost:8081/orders/{orderId}

# Restaurar inventory service
curl -X POST "http://localhost:8083/test/failure?enable=false"
```

**Logs esperados:**
```
[SAGA:xxx] Starting order creation
[SAGA:xxx] Order created with status PENDING
[SAGA:xxx] Calling payment-service
[SAGA:xxx] Payment SUCCESS, paymentId: PAY-yyy
[SAGA:xxx] Calling inventory-service
[SAGA:xxx] Inventory reservation FAILED (simulated)
[SAGA:xxx] Compensating: refunding payment
[SAGA:xxx] Payment refunded
[SAGA:xxx] Order CANCELLED
```

---

### 4. Múltiples órdenes en paralelo

```bash
# Crear varias órdenes rápidamente
for i in {1..5}; do
  curl -X POST http://localhost:8081/orders \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"user-$i\", \"total\": $((50 + i * 10))}" &
done
wait

# Revisar logs para ver múltiples sagas ejecutándose
```

---

## 🗄️ Base de Datos H2 Console

Order Service usa H2 con consola habilitada:

**URL:** http://localhost:8081/h2-console

**Configuración:**
- JDBC URL: `jdbc:h2:mem:orderdb`
- User: `sa`
- Password: *(vacío)*

**Consultar órdenes:**
```sql
SELECT * FROM ORDERS;
```

---

## 📊 Estructura del Proyecto

```
product-management/
├── order-service/
│   ├── src/main/java/org/barcodev/orderservice/
│   │   ├── OrderServiceApplication.java
│   │   ├── model/
│   │   │   ├── Order.java
│   │   │   └── OrderStatus.java
│   │   ├── repository/
│   │   │   └── OrderRepository.java
│   │   ├── dto/
│   │   │   ├── CreateOrderRequest.java
│   │   │   ├── PaymentRequest.java
│   │   │   ├── PaymentResponse.java
│   │   │   ├── RefundRequest.java
│   │   │   ├── InventoryRequest.java
│   │   │   ├── InventoryResponse.java
│   │   │   └── ReleaseRequest.java
│   │   ├── client/
│   │   │   └── ServiceClient.java
│   │   ├── service/
│   │   │   └── OrderService.java (SAGA ORCHESTRATOR)
│   │   └── controller/
│   │       └── OrderController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── build.gradle
│
├── payment-service/
│   ├── src/main/java/org/barcodev/paymentservice/
│   │   ├── PaymentServiceApplication.java
│   │   ├── dto/
│   │   │   ├── PaymentRequest.java
│   │   │   ├── PaymentResponse.java
│   │   │   └── RefundRequest.java
│   │   ├── service/
│   │   │   └── PaymentService.java (30% failure rate)
│   │   └── controller/
│   │       ├── PaymentController.java
│   │       └── TestController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── build.gradle
│
├── inventory-service/
│   ├── src/main/java/org/barcodev/inventoryservice/
│   │   ├── InventoryServiceApplication.java
│   │   ├── dto/
│   │   │   ├── InventoryRequest.java
│   │   │   ├── InventoryResponse.java
│   │   │   └── ReleaseRequest.java
│   │   ├── service/
│   │   │   └── InventoryService.java (20% failure rate)
│   │   └── controller/
│   │       ├── InventoryController.java
│   │       └── TestController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── build.gradle
│
├── build.gradle (root)
├── settings.gradle
└── README.md
```

---

## 🔍 Observabilidad - Preparación

Esta aplicación está diseñada para agregar:

- **OpenTelemetry** (trazas distribuidas)
- **Prometheus** (métricas)
- **Loki/ELK** (logs centralizados)

Los headers `X-Saga-Id` y `X-User-Id` ya están propagándose correctamente, listos para correlación de trazas.

---

## ⚙️ Configuraciones

### Puertos

| Servicio | Puerto |
|----------|--------|
| order-service | 8081 |
| payment-service | 8082 |
| inventory-service | 8083 |

### Tasa de fallos

| Servicio | Tasa |
|----------|------|
| payment-service | 30% |
| inventory-service | 20% |

---

## 📝 Logs

Los logs incluyen el `sagaId` para rastreo:

```
[SAGA:a1b2c3d4] Starting order creation
[SAGA:a1b2c3d4] Payment SUCCESS
[SAGA:a1b2c3d4] Inventory reservation SUCCESS
[SAGA:a1b2c3d4] Order CONFIRMED
```

En caso de fallo:

```
[SAGA:x1y2z3w4] Inventory reservation FAILED
[SAGA:x1y2z3w4] Compensating: releasing inventory
[SAGA:x1y2z3w4] Compensating: refunding payment
[SAGA:x1y2z3w4] Order CANCELLED
```

---

## 🎯 Resumen

- ✅ Saga orquestada implementada en `OrderService`
- ✅ Compensaciones automáticas (refund, release)
- ✅ Headers propagados (X-Saga-Id, X-User-Id)
- ✅ Simulación de fallos (aleatorios y forzados)
- ✅ Persistencia solo en order-service (H2)
- ✅ Comunicación REST sincrónica
- ✅ Preparado para observabilidad futura

---

## 🧰 Comandos Útiles

```bash
# Compilar todo
./gradlew clean build

# Ejecutar tests
./gradlew test

# Ver dependencias
./gradlew dependencies

# Limpiar build
./gradlew clean
```

---

## 📚 Próximos Pasos (Observabilidad)

1. Agregar **OpenTelemetry** para trazas distribuidas
2. Instrumentar con **Micrometer** para métricas
3. Configurar **Prometheus** y **Grafana**
4. Implementar logs estructurados con correlación de sagaId
5. Agregar **health checks** y **circuit breakers**

---

## 🛠️ Troubleshooting

### Error: Puerto ya en uso

```bash
# Linux/Mac
lsof -i :8081
kill -9 <PID>

# Windows
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Error: Servicios no se comunican

Verifica que todos los servicios estén corriendo:
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Ver logs detallados

Editar `application.yml` de cada servicio:
```yaml
logging:
  level:
    org.barcodev: DEBUG
```

---

## 📞 Contacto

Desarrollado para demostración de arquitectura de microservicios con saga orquestada.