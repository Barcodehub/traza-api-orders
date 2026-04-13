# Dockerización y Observabilidad de Logs con Datadog

Este documento detalla el proceso mediante el cual dockerizamos nuestra aplicación de microservicios y cómo configuramos la recolección integral de logs (además de métricas y trazas) hacia Datadog.

## 1. Dockerización de los Microservicios

Para lograr que los servicios de Order, Payment e Inventory se ejecutaran en contenedores, y pudieran intercomunicarse con herramientas de infraestructura adicionales, realizamos:

### 1.1. Los Archivos `Dockerfile`
En cada microservicio (`order-service`, `payment-service`, `inventory-service`) agregamos un `Dockerfile` estandarizado para entornos Java:
- **Imagen Base:** Empleamos `eclipse-temurin:17-jre-jammy`, la cual es una versión ligera, segura y oficial basada en Ubuntu.
- **Seguridad (Non-root user):** Añadimos un grupo y usuario llamado `spring` para evitar que la aplicación corra como `root` dentro del contenedor. Esto es una buena práctica crítica de seguridad.
- **Artefactos:** Definimos un `ARG JAR_FILE` dinámico que toma el `.jar` compilado en `./build/libs/`.
- **Entrypoint:** Preparamos el comando estándar `java -jar /app.jar` para inicializar Spring Boot.

### 1.2. El `docker-compose.yml`
En la carpeta `infra/` armamos un orquestador local para levantar absolutamente todo con un solo comando:
- Se declararon las builds para nuestros tres servicios Spring Boot apuntando a sus carpetas padre (`../order-service`, etc.).
- Definimos variables de entorno esenciales:
  - `SPRING_PROFILES_ACTIVE=docker`
  - Perfiles relacionados a Datadog (`OTEL_EXPORTER_OTLP_ENDPOINT`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`).
- Indicamos correctamente las dependencias (`depends_on`) para asegurar que todo arranque después de los brokers de telemetría y bases de datos.

## 2. Observabilidad de Logs hacia Datadog

Previamente, nuestra aplicación exportaba Trazas y Métricas a Datadog vía OpenTelemetry (`DD_APM_ENABLED=true`), pero **los logs de aplicación no se estaban mandando correctamente a la nube**.

Para arreglar esto, modificamos el servicio de Datadog Agent en nuestro `docker-compose.yml` e introducimos el auto-descubrimiento de Docker:

### 2.1. Ajustes en el Contenedor del Agente Datadog
Agregamos variables de entorno vitales para que el agente escupiera y escaneara los logs nativos de los contenedores Docker:
```yaml
- DD_LOGS_ENABLED=true
- DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true
- DD_LOGS_CONFIG_AUTO_MULTI_LINE_DETECTION=false
```
Además, exponemos el socket de Docker y los volúmenes para que Datadog pudiera leer en vivo el estándar I/O de nuestras apps Java:
```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock:ro
  - /var/lib/docker/containers:/var/lib/docker/containers:ro
```

### 2.2. Autodiscovery (Labels en Docker)
Para asegurar que los logs se identifiquen y procesen en Datadog explícitamente como originados de aplicaciones Spring Boot (facilitando el parsing del TraceId/SpanId dentro del formato del log), pasamos labels específicos de autodiscovery (`ad`) en cada uno de nuestros contenedores microservicios:
```yaml
labels:
  com.datadoghq.tags.service: "order-service"
  com.datadoghq.tags.env: "dev"
  com.datadoghq.ad.logs: '[{"source": "spring-boot", "service": "order-service"}]'
```
De esta manera, Datadog automáticamente parsea nuestro patrón de Logback (que incluye `traceId`, `eventId`, `correlationId`, etc.) y es capaz de indexarlo junto con la traza de OpenTelemetry en la misma consola en vivo.
