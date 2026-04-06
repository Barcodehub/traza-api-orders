# Observabilidad Centralizada con Datadog y OpenTelemetry

Este documento describe la configuración e instrumentación de observabilidad para los microservicios (`inventory-service`, `order-service` y `payment-service`) utilizando **Datadog** como APM principal. El objetivo fue centralizar Logs, Métricas y Trazas en un solo lugar, prescindiendo de herramientas adicionales como Prometheus o Grafana.

## 1. Arquitectura e Implementación Técnica

Para lograr una integración agnóstica al proveedor (Cloud Native), evitamos usar el agente propietario de Datadog (`dd-java-agent`) en nuestra aplicación y optamos por el estándar abierto **OpenTelemetry (OTLP)** combinado con **Spring Boot Micrometer**.

### 1.1 Datadog Agent (Docker)
Utilizamos la imagen oficial `gcr.io/datadoghq/agent` corriendo mediante `docker-compose`. El agente se configuró para:
* Recibir telemetría OTLP a través de los puertos `4317` (gRPC) y `4318` (HTTP).
* Habilitar la recolección de Logs de contenedores en Docker (`DD_LOGS_CONFIG_CONTAINER_COLLECT_ALL=true`).
* Habilitar APM (`DD_APM_ENABLED=true`).

### 1.2 Instrumentación en Spring Boot (Build.gradle)
A cada microservicio se le añadieron las siguientes dependencias clave:
* `io.opentelemetry:opentelemetry-exporter-otlp`: Para exportar las trazas.
* `io.micrometer:micrometer-tracing-bridge-otel`: Para crear el puente de trazas de micrometer con OpenTelemetry.
* `io.micrometer:micrometer-registry-otlp`: **Vital para enviar métricas** (CPU, HTTP hits, custom metrics) a Datadog usando el protocolo OTLP.

### 1.3 Configuración Base (`application.yml`)
En lugar de mandar métricas a un endpoint de Prometheus o Jaeger, todo el tráfico se unificó apuntando al colector OTLP del Agente de Datadog local:
```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    enabled: true
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces 
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
        step: 10s
```

## 2. Manejo de Errores y Patrón Saga (Business vs Server Errors)

Uno de los principales retos configurados fue diferenciar fallos controlados de negocio (Ej: Patrón Saga revirtiendo una transacción por falta de inventario) respecto a fallos críticos de servidor (HTTP 500).
* Cuando `inventory-service` o `payment-service` fallan intencionalmente como parte de la simulación, capturamos el evento usando `tracer.currentSpan().error(e)`. 
* Esto colorea el _Span_ específicamente como **ERROR (Rojo)** en Datadog, a pesar de que el servicio responde bajo un flujo transaccional controlado (HTTP 200). 
* El orquestador (`order-service`) registra las compensaciones como un simple _evento de éxito en el negocio_, manteniendo limpio el monitoreo global.

---

## 3. Guía de Uso: Interfaz de Datadog (UI)

Gracias a la instrumentación anterior, podemos explotar toda la potencia de la UI de Datadog.

![Captura de pantalla 2026-04-06 143531.png](updates/public/Captura%20de%20pantalla%202026-04-06%20143531.png)

### A. Trazas (Trace Explorer)
Aquí podemos ver el **Flame Graph** del Patrón Saga al completo. Para buscar errores específicos dentro de las trazas o filtrar por comportamientos:
* Vamos a **APM -> Traces**.
* Filtramos usando los tags de OpenTelemetry, por ejemplo: `@userId:user-fail status:error`

![Captura de pantalla 2026-04-06 151756.png](updates/public/Captura%20de%20pantalla%202026-04-06%20151756.png)

### B. Métricas (Metrics Explorer)
Olvidamos las viejas métricas `trace.*` y utilizamos los nombres estandarizados generados por **Micrometer**:
* Vamos a **Metrics -> Explorer**.
* **Tráfico HTTP (Hit rate):** Graficamos `http.server.requests` (seleccionando la función matemática `count` en vez de `avg`), agrupado (`avg by`) por `uri` o `status`.
* **Latencia / Rendimiento:** Graficamos `http.server.requests` usando la función `p95`.

![Captura de pantalla 2026-04-06 170349.png](updates/public/Captura%20de%20pantalla%202026-04-06%20170349.png)
![Captura de pantalla 2026-04-06 170726.png](updates/public/Captura%20de%20pantalla%202026-04-06%20170726.png)

### C. Dashboards
Centralizamos el estado (Salud de infraestructura + KPIs de Negocio) en un solo panel para nuestra "War Room":
* Vamos a **Dashboards -> New Dashboard**.
* En el panel combinamos datos de JVM (`jvm.memory.used`), peticiones HTTP, y las métricas **propias de negocio** creadas en código: `orders.created.total` y `saga.failed.total`.

![Captura de pantalla 2026-04-06 172802.png](updates/public/Captura%20de%20pantalla%202026-04-06%20172802.png)
![Captura de pantalla 2026-04-06 172817.png](updates/public/Captura%20de%20pantalla%202026-04-06%20172817.png)

### D. Monitores (Alertas Automatizadas)
Dejamos de buscar métricas manualmente y ordenamos al sistema que nos avise:
* Vamos a **Monitors -> New Monitor -> Metric**.
* Seteamos una alerta que monitorice en vivo la métrica `saga.failed.total`.
* Si las fallas exceden un umbral especifico en los últimos 5 minutos, Datadog notifica al equipo automáticamente para revisar si un servicio subyacente está comprometido.

![Captura de pantalla 2026-04-06 173150.png](updates/public/Captura%20de%20pantalla%202026-04-06%20173150.png)
![Captura de pantalla 2026-04-06 174054.png](updates/public/Captura%20de%20pantalla%202026-04-06%20174054.png)