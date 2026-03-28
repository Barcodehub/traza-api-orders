# Update 1.0: Propagación Manual de Contexto y Trazabilidad Básica

## Estado Inicial: ¿Cómo lo teníamos antes?
Antes de la refactorización con MDC, la propagación del contexto (`sagaId` y `userId`) entre nuestros microservicios (`order-service`, `payment-service`, `inventory-service`) se realizaba de forma explícita y manual en el código.

1. **En el cliente (`order-service`):** El cliente HTTP enviaba los IDs pasándolos por parámetro en las firmas de los métodos del `ServiceClient`. Esto obligaba a construir un objeto `HttpHeaders` manualmente para inyectar `X-Saga-Id` y `X-User-Id` en cada llamada de `RestTemplate`.
2. **En los controladores receptores:** Tanto `PaymentController` como `InventoryController` capturaban explícitamente estas cabeceras usando anotaciones de Spring:
   * `@RequestHeader(value = "X-Saga-Id") String sagaId`
   * `@RequestHeader(value = "X-User-Id") String userId`
3. **En la lógica de negocio (Servicios):** Los controladores arrastraban estos valores hasta la capa de servicios, donde los métodos recibían `(Request request, String sagaId, String userId)` simplemente para poder imprimirlos en los `log.info()`.

## ¿Cuál era su función (Para qué nos funcionaba)?
El propósito central era lograr la **trazabilidad de transacciones distribuidas (Implementando el patrón Saga)**. 
Al compartir un mismo `sagaId` a través de los múltiples servicios desencadenados por el `order-service`, podíamos rastrear en los logs el ciclo de vida completo de una orden ("Reserva de inventario", "Procesamiento de pago", "Limpieza y compensación"). Esto nos permitía identificar exactamente dónde, cuándo y por qué fallaba o triunfaba una petición de negocio distribuida, uniendo el rompecabezas de logs separados.

## ¿Por qué lo hicimos así inicialmente?
* **Simplicidad y transparencia absoluta:** Es el método más directo y fácil de entender. Al estar explícito en los parámetros, era evidente que esos datos viajaban entre métodos; no había "código mágico".
* **Prueba de concepto funcional:** Solucionaba de inmediato nuestra necesidad de correlacionar logs sin tener que instalar componentes complejos.
* **Cero dependencias y librerías externas:** Nos permitió construir las Sagas sin requerir frameworks avanzados de tracing (como OpenTelemetry, Zipkin o Spring Cloud Sleuth).

Aunque fue perfecto para empezar e implementar el modelo fundacional de las Sagas, el código se tornó repetitivo y poco mantenible, lo que dio paso a la refactorización (Update 1.1).
