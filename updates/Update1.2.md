# Update 1.2: Logs Estructurados (JSON) y Resolución de Vulnerabilidades

En esta actualización, evolucionamos el sistema de logs para que emita todo en formato estructurado JSON. Esto preparará nuestras aplicaciones para ser integradas fácilmente con herramientas de observabilidad modernas como ELK, Datadog o Grafana. Además, solventamos de forma preventiva vulnerabilidades heredadas por librerías de terceros (CVEs).

## Cambios realizados

### 1. Inclusión de `logstash-logback-encoder`
- Se añadió la dependencia `net.logstash.logback:logstash-logback-encoder:9.0` a los archivos `build.gradle` de todos los microservicios (`order-service`, `payment-service` e `inventory-service`).
- Esta librería se encarga de interceptar y formatear la salida estándar a JSON puro, mapeando automáticamente el **Mapped Diagnostic Context (MDC)**.

### 2. Configuración de Logs en `logback-spring.xml`
Se creó el archivo `src/main/resources/logback-spring.xml` en cada proyecto con la siguiente funcionalidad:
- **Estructuración:** Los logs ahora imprimen de forma ordenada nivel, mensaje, logger, y la fecha (`@timestamp`).
- **Campos personalizados fijos:** Se incluyeron los campos `"service"` (p.ej.: `order-service`) y `"environment"` (`dev`) para identificar de dónde proviene cada traza rápidamente en sistemas centralizados.
- **Mapeo automático de Transacciones (MDC):** Nuestras llaves preexistentes (`sagaId` y `userId`) ahora aparecen nativamente en la raíz del JSON sin necesidad de concatenarlas en los mensajes.
- **Enmascaramiento de seguridad (Bonus):** Se configuró un `<valueMasker>` mediante Expresiones Regulares para ocultar de forma automática datos sensibles (como `password`, `token` o `bearer`) reemplazándolos con `***`.

### 3. Resolución de Conflictos y Vulnerabilidades en `Jackson` (CVEs)
- **Problema encontrado:** La versión 9.0 del encoder arrastraba una versión vulnerable de `jackson-core` en su árbol transitivo de dependencias, lo cual marcaba problemas de "Resource Exhaustion" (CVE-2026-29062).
- **El parche inicial:** Intentamos actualizar la rama `tools.jackson.core` (3.x), lo que provocó errores de inicialización y `NoClassDefFoundError` en el entorno Spring Boot 3 que sigue apoyándose en la rama de anotaciones `2.x`.
- **Solución final y segura:** Utilizamos **Gradle Constraints** para anclar todas las dependencias transitivas de Jackson a la capa segura `com.fasterxml.jackson.core` **versión `2.16.1`**. De este modo mitigamos todas las vulnerabilidades detectadas manteniendo la total compatibilidad con el entorno y el compilador de Spring Boot.

### Ejemplo de Salida Final (Log estructurado)
```json
{
  "@timestamp": "2026-03-28T21:37:30.407-05:00",
  "message": "Starting ProtocolHandler [\"http-nio-8082\"]",
  "logger_name": "org.apache.coyote.http11.Http11NioProtocol",
  "thread_name": "main",
  "level": "INFO",
  "service": "payment-service",
  "environment": "dev"
}
```

Gracias a este cambio, nuestras Sagas y transacciones distribuidas ahora están listas para ser enviadas a herramientas de Elastic Search sin necesidad de aplicar transformaciones previas logrando una observabilidad resiliente y segura.
