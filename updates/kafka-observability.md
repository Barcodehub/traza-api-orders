# Arquitectura de Eventos (Kafka) y Observabilidad Completa

Este documento profundiza en la configuración introducida en nuestros microservicios para emplear Eventos Asíncronos vía Apache Kafka, y más importante aún, el intrincado diseño para rastrear esos eventos a través de **Traces y Logs en Datadog** (Traceability).

## 1. Integración con Apache Kafka (Docker Compose)

Antes de hacer nada en Java, agregamos al `docker-compose.yml` los servicios **Zookeeper** y **Kafka**.
Kafka fue provisto usando la imagen `confluentinc/cp-kafka`. Para que los agentes Java pudieran encontrar a los brokers desde el contenedor, usamos `0.0.0.0:29092` internamente y establecimos la comunicación general de red hacia el host local. Los servicios dependientes (`order`, `inventory`, `payment`) fueron enlazados a estos brokers en su `bootstrapping` enviando por variable de entorno:
```yaml
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

## 2. Emisión de Eventos (KafkaTemplate y OrderController)

En `order-service`, agregamos dos nuevos endpoints:
1. `GET /api/kafka/send`: Inicializa un evento asíncrono y crea la primera publicación hacia `order-events-topic`.
2. `GET /api/kafka/send-fail`: Funciona de manera similar, pero inserta un "FAIL_ON_PURPOSE" dentro de la carga útil del evento, simulando una caída a propósito en los servicios receptores.

En la capa de servicio/controlador, configuramos el `KafkaTemplate` provisto por Spring Boot para despachar vía:
```java
ProducerRecord<String, Object> record = new ProducerRecord<>(
    "order-events-topic", null, request.getUserId(), event
);
kafkaTemplate.send(record)
```

## 3. Trazabilidad Original: Inyectando Contexto

Para la trazabilidad distribuida necesitamos pasar los Ids de operación (por ejemplo: `eventId` y `userId`) dentro y fuera de Kafka. Esto se logra mediante **Interceptors** e inserciones al `MDC`.

### 3.1. Producer Interceptor (Order Service)
Añadimos `KafkaTracingProducerInterceptor`. Su función es tomar los valores actuales del `MDC` que definimos en nuestro endpoint web, incrustarlos directamente a nivel binario en los `Headers` de Kafka (ej: `X-Correlation-Id`), y registrar dichas etiquetas al Span de OTel actual (`Span.current().setAttribute(...)`). Esto asegura que el "vuelo" en la red de Kafka tenga la meta-dirección inicial.

### 3.2. Observabilidad de OTel desde Spring (ObservationConfig)
El problema principal surge con los Listeners (Inventory y Payment). **Desacoplar la Telemetría de la Lógica de Negocio** es crítico.

Para no ensuciar nuestro manejador o método `@KafkaListener` con el infame código `Span.current()...`, inyectamos este código en una política limpia en `ObservationConfig.java` dentro de cada servicio consumidor (payment, inventory):
```java
@Bean
public ObservationFilter kafkaObservationFilter() {
    return context -> {
        if (context instanceof KafkaRecordReceiverContext kafkaContext) {
            var record = kafkaContext.getRecord();
            var correlationHeader = record.headers().lastHeader("X-Correlation-Id");
            if (correlationHeader != null) {
                // Inyecta dinámicamente nuestra etiqueta 'eventId' al Span Activo (Trace OTel)
                context.addHighCardinalityKeyValue(KeyValue.of("eventId", new String(correlationHeader.value())));
            }
        }
        return context;
    };
}
```
Esto interceptará todos los mensajes mágicamente y adjuntará los IDs al Span de OpenTelemetry en el momento exacto en el que el Thread de Kafka-Listener comience a procesar.

### 3.3. Observabilidad a Nivel de Logs: MDC (Mapped Diagnostic Context)
Si bien el componente OTel trazaba todo en UI, los LOGS no sabían quién generaba el Log de la consola, impidiendo realizar agrupaciones efectivas. Por consiguiente se crearon:

1. **`KafkaMdcInterceptor`**: Se ata directamente como un interceptor al Consumidor (`RecordInterceptor`). Extrae las cabeceras Kafka (`X-...`) y enriquece el diccionario local de subprocesos Java para Logback:
   `MDC.put("eventId", headerVal);`
   
Consecuentemente, los Loggers (`log.info(...)`) plasmaron cada nuevo string al `application.yml` (`%X{eventId}`).

## 4. Fallos Esperados y Trazabilidad de Errores (Retrying)

Los endpoints `/send-fail` desencadenan excepciones no capturadas al invocar un `throw new RuntimeException()`.
Kafka, por defecto, reintenta (re-polls), pero esos hilos reintentados (en su background de error handler) no conservaban nuestro contexto `MDC`, lo que resultaba en fallas no traceadas en Datadog, mostrando "disconnection" repetida de flujos de logs.

### 4.1. Solución: DefaultErrorHandler con RetryListeners
En nuestro archivo general `KafkaConfig.java` (presente en servicios Inventory y Payment), implementamos un Handler `DefaultErrorHandler` con un `FixedBackOff` (por ejemplo, 3 reintentos espaciados).
Ahí le añadimos el gancho clave (`setRetryListeners`), el cual extrae los headers del registro dañado (que falló y está iterando para reintento final):
```java
errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
    var correlationHeader = record.headers().lastHeader("X-Correlation-Id");
    if (correlationHeader != null) {
        MDC.put("eventId", new String(correlationHeader.value(), StandardCharsets.UTF_8));
    }
});
```
Además, enrutamos el mensaje final a un Topic `DLT` (Dead Letter Topic) mediante el `DeadLetterPublishingRecoverer`.

## Conclusión y Aprendizaje
1. **Para Trazas (Spans/OTel):** Spring Boot 3 provee un `ObservationFilter` que inspecciona contextos implícitos de Kafka (`KafkaRecordReceiverContext`), esto evita tocar el código del Listener y mapea los Headers del Payload en Tags útiles en Datadog de manera transparente.
2. **Para Logs Clásicos:** El `MDC` maneja el tracking. Un `RecordInterceptor` de Kafka es suficiente para las recepciones estándar, mientras que un `RetryListener` inyectado en la política de error es mandatorio para garantizar el MDC durante las reconexiones o el volcado de excepciones de back-off.
