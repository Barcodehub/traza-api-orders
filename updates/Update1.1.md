# Update 1.1: Refactorización de Propagación de Contexto (Trazabilidad)

En esta actualización se mejoró la trazabilidad de las peticiones a través de los microservicios usando el **Contexto de Diagnóstico Mapeado (MDC)**. El objetivo principal fue eliminar el paso manual y repetitivo de cabeceras (`X-Saga-Id`, `X-User-Id`) por cada método y centralizar su manejo.

## Cambios realizados

### 1. `order-service` (Origen de la Petición/Saga)
- **Creación de `HeaderPropagationInterceptor`:** Implementa `ClientHttpRequestInterceptor`. Intercepta toda petición HTTP saliente de `RestTemplate`, lee `sagaId` y `userId` del hilo actual (`MDC.get()`) y los inyecta automáticamente en los `headers` de la request.
- **Configuración de `RestTemplate`:** Se registró el `HeaderPropagationInterceptor` en el Bean de `RestTemplate` dentro de `OrderServiceApplication`.
- **Refactor en `ServiceClient`:** Se eliminaron los parámetros `sagaId` y `userId` en las llamadas a `payment-service` e `inventory-service`. Ahora el cliente solo pasa el objeto `Request`.
- **Uso de MDC en `OrderService`:** Al iniciar una saga (ej. `createOrder`), se guardan el `sagaId` y el `userId` en el MDC (`MDC.put(...)`). Se utiliza un bloque `try/finally` asegurando que al terminar el proceso, el contexto se limpie (`MDC.clear()`) para no contaminar la reutilización de hilos.

### 2. `payment-service` y `inventory-service` (Receptores)
- **Creación de `MdcLoggingFilter`:** En ambos servicios se creó un filtro HTTP (que hereda de `OncePerRequestFilter`). Este filtro captura automáticamente los headers `X-Saga-Id` y `X-User-Id` de la petición entrante y los deposita en el MDC local del servicio. También asegura la limpieza con un `try/finally { MDC.clear(); }`.
- **Refactor de Controladores (`PaymentController` y `InventoryController`):** Se eliminaron las anotaciones `@RequestHeader` en los endpoints, limpiando la firma de los métodos.
- **Refactor de Servicios (`PaymentService` e `InventoryService`):** Ya no reciben los IDs por parámetro. Obtienen la información necesaria para los logs u otras operaciones directamente consultando `MDC.get("sagaId")` y `MDC.get("userId")`.


### Resumen: 
- creacion de interceptores y filtros para manejar automáticamente la propagación de contexto, eliminando la necesidad de pasar explícitamente los IDs en cada método y centralizando su gestión en el MDC.


## Beneficios de este avance
- **Código más limpio:** Las firmas de los métodos solo reciben la información de negocio relevante (el Payload/Request).
- **Trazabilidad implícita en logs:** Al usar MDC, cualquier log que incluya los keys del MDC imprimirá los datos de rastreo automáticamente, sin pasarlos directamente al método de la capa lógica.
- **Escalabilidad:** Si en el futuro es necesario agregar más headers de trazabilidad (ej., `Trace-Id` o datos del tenant), solo hay que agregarlos en los interceptores/filtros sin tocar los controladores o los servicios.
