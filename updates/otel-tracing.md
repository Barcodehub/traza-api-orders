# Actualización 1.5 - Trazabilidad Distribuida con OpenTelemetry y Jaeger
Hemos incorporado observabilidad de extremo a extremo (End-to-End Tracing) para poder seguir el ciclo de vida completo de las peticiones a través de nuestros microservicios usando OpenTelemetry y Jaeger.

- Jeager se usa para visualizar datos de trazas.
- OpenTelemetry es el estándar abierto para instrumentar, generar, recolectar y exportar datos de observabilidad (trazas, métricas y logs).
- Se pueden incluir campos personalizados en los logs (como sagaId y userId) en los spans de OpenTelemetry, lo que permite correlacionar fácilmente los logs con las trazas.
- En Jeager UI no se pueden hacer queries complejas sobre los logs, no puedes hacer userId AND error=true. Para eso es mejor usar Kibana, pero Jaeger es excelente para entender la latencia y el flujo de las peticiones.

## 1. Dependencias Incorporadas
Añadimos las siguientes dependencias en los archivos build.gradle de **todos** los microservicios:
```groovy
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
```
- **micrometer-tracing-bridge-otel**: Actúa sobre los componentes manejados por Spring (RestTemplate, Spring MVC, etc.) y genera Spans (Trazas) inyectando los `traceId` y `spanId`.
- **opentelemetry-exporter-otlp**: Exporta las trazas recolectadas usando el estándar OTLP (OpenTelemetry Protocol) para enviarlas a sistemas externos (en nuestro caso Jaeger).
## 2. Refactor del RestTemplate (Clave para Propagar Trazas)
En order-service, modificamos la forma en que se crea el bean de RestTemplate para utilizar el RestTemplateBuilder:
```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder, HeaderPropagationInterceptor interceptor) {
    return builder.additionalInterceptors(interceptor).build();
}
```
*¿Por qué?* Spring Boot inyecta automáticamente los interceptores de OpenTelemetry **solo** si usas el RestTemplateBuilder. Esto asegura que el trace_id viaje en los headers HTTP a payment-service e inventory-service sin perder el contexto que ya teníamos de nuestro HeaderPropagationInterceptor.
## 3. Configuración en application.yml
Agregamos esto a los tres microservicios:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0 # Para capturar el 100% de las peticiones en Dev.
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces # Ruta hacia el OTLP HTTP receiver de Jaeger.
```
## 4. Infraestructura (docker-compose.yml)
Ya contabas con Jaeger configurado para ejecutarse. Al levantar el Docker, los servicios enviarán trazas al puerto 4318.
Para levantar Jaeger de manera normal con todo el stack:
```bash
docker-compose up -d
```
## 5. Visualizar Trazas en Jaeger
1. Accede a http://localhost:16686 en tu navegador.
2. En la barra izquierda selecciona el **Service** (por ejemplo, order-service).
3. Clickea **Find Traces**.
4. Verás las trazas ordenadas por más recientes. Observarás gráficos de cuánto tiempo pasa la petición en order-service, cómo llama a payment-service y luego a inventory-service.

## 6. Correlación: trace_id, sagaId y userId
Gracias a logstash-logback-encoder y Micrometer Tracing:
Cada log JSON en la consola de tus servicios ahora va a incluir automáticamente los campos "traceId" y "spanId".
**Flujo:**
- Entra una petición a order-service: Se genera un traceId.
- El MDC también captura nuestro el UUID en sagaId y userId.
- Toda la traza comparte el **mismo** traceId, lo que permite ver la rama técnica de ejecución en Jaeger.
- Si vas a los logs (Kibana o simplemente en consola), puedes buscar por "traceId" o por "sagaId". El traceId enlaza todo el árbol de peticiones HTTP, mientras que el sagaId enlaza la lógica de negocio completa.

## Filtro de observabilidad
- No ver trazas de endpoints como `/actuator/health` o `/actuator/prometheus` en directorio config/ ObservationConfig.java

## Spans enriquecidos con userId y sagaId para buscarlos en Jaeger por ejemplo por sagaId=1234 en el filtro de búsqueda de Jaeger en tags.
- He enriquecido los spans de Jaeger con el userId y sagaId inyectando el componente Tracer de OpenTelemetry y etiquetando los spans actuales en los MdcLoggingFilter de payment-service e inventory-service, así como en la creación de la petición en order-service.
- Además, he modificado la lógica en el OrderController y en el OrderService para que, cuando envíes una petición (request) donde el monto total sea negativo (por ejemplo, -10), lance una excepción forzando así un error HTTP 500.  Este error 500 agregará las etiquetas correspondientes con el evento del error a la traza, por lo cual deberías ver el recuadro del span en rojo dentro de la interfaz gráfica de Jaeger.


## BONUS: ¿Cómo cambiar a Datadog, New Relic o Dynatrace sin tocar código?
El uso del estándar OTLP permite que nuestra aplicación sea "Agnóstica al Agente de Monitoreo". No tienes que instalar dependencias propietarias de Datadog en el código Java.
Solo bastaría con usar el agente "OpenTelemetry Collector", y cambiarías sus propiedades (credenciales / tokens del proveedor cloud).
De este modo, configuras tu servicio simplemente apuntando la IP: `endpoint: http://datadog-agent:4318/v1/traces` y los datos aterrizarán en Datadog. ¡Cero cambios en tu código de negocio!
