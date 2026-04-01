# Laboratorio de Observabilidad y Trazabilidad

Este proyecto es un entorno de prueba diseñado para demostrar cómo centralizar y correlacionar los tres pilares de la observabilidad en una arquitectura de microservicios usando exclusivamente herramientas **Open Source**.

Tenemos un ecosistema de 3 microservicios (Order, Payment e Inventory) completamente instrumentados para enviar datos de telemetría y diagnósticos.


## Arquitectura del Stack de Observabilidad

| Pilar | Herramientas Utilizadas | Propósito |
| :--- | :--- | :--- |
| **Logs (Registros)** | ELK Stack (Elasticsearch, Kibana, Filebeat) | Centralizar todos los eventos y errores. Correlacionados mediante `traceId`, `spanId` y `sagaId` (MDC). |
| **Métricas** | Micrometer, Prometheus, Grafana | Analizar salud del JVM, contadores personalizados y métricas HTTP (tasas de error, tiempos de respuesta). |
| **Trazas (Tracing)** | OpenTelemetry, Jaeger | Visualizar el flujo y cuellos de botella de una petición (request) a medida que viaja de un microservicio a otro. |

---

# Stack de Observabilidad Open Source Completo 📊🔍

En este proyecto hemos implementado exitosamente un stack completo de observabilidad basado en componentes de código abierto que nos permite monitorear y entender en profundidad el comportamiento de nuestros 3 microservicios (`order-service`, `payment-service` e `inventory-service`).

Esta arquitectura de observabilidad se divide en los 3 pilares clave (Logs, Métricas y Trazas) corriendo en un contenedor de pruebas (`infra/`).

## 1. Logs Centralizados → ELK Stack (Elasticsearch, Kibana, Filebeat)

Al tener múltiples servicios, leer la consola independientemente se vuelve imposible. Centralizamos todos los registros generados.

* **Componentes:** 
  * `Filebeat`: Agente recolector de archivos `.log`.
  * `Elasticsearch`: Base de datos orientada a documentos que almacena el formato JSON estructurado de los logs.
  * `Kibana`: Panel web interactivo para búsqueda (`http://localhost:5601`).
* **Valor agregado:** Formato JSON y propagación de variables MDC (`sagaId`, `userId`) usando _Logstash Logback Encoder_. Permite filtrar qué incidencias de negocio pasaron según el usuario o flujo del request sin importar a qué servicio llegaron.

![Captura de pantalla 2026-03-30 174426.png](updates/public/Captura%20de%20pantalla%202026-03-30%20174426.png)


## 2. Métricas de Rendimiento → Prometheus + Grafana

* **Componentes:**
  * `Micrometer y Actuator`: Exponen el endpoint nativo en `/actuator/prometheus` en Spring Boot.
  * `Prometheus`: Servidor de recolección de series de tiempo cada corto período de segundos.
  * `Grafana`: Dashboard de visualización e interfaces gráficas para centralizar reportes (`http://localhost:3000`).
* **Valor agregado:** Observamos la tasa de éxito de creación de "Orders", conteo de fallos de las Sagas, el tiempo de respuesta HTTP promedio, contabilidad de errores HTTP 500 y cuellos de botella de hardware de JVM (CPU/RAM).

![Captura de pantalla 2026-03-30 111928.png](updates/public/Captura%20de%20pantalla%202026-03-30%20111928.png)
![Captura de pantalla 2026-03-30 111916.png](updates/public/Captura%20de%20pantalla%202026-03-30%20111916.png)
![Captura de pantalla 2026-03-30 111754.png](updates/public/Captura%20de%20pantalla%202026-03-30%20111754.png)
![Captura de pantalla 2026-03-30 111903.png](updates/public/Captura%20de%20pantalla%202026-03-30%20111903.png)
![Captura de pantalla 2026-03-30 111617.png](updates/public/Captura%20de%20pantalla%202026-03-30%20111617.png)


## 3. Trazas Distribuidas → Jaeger (OpenTelemetry)

* **Componentes:**
  * `OpenTelemetry (OTLP)`: Instrumentación distribuida de Java con Micrometer Tracing API.
  * `Jaeger`: Sistema recolector, almacenador y panel visual de cada paso de ejecución sub-divididiéndolo (`http://localhost:16686`).
* **Valor agregado:** Visualizar una solicitud HTTP compleja como una sola "Traza", subdividida en varios "Spans". Nos sirve enormemente para encontrar quién fue el responsable de un aumento drástico en el tiempo de procesamiento o encontrar un cuello de botella entre microservicios.

---![Captura de pantalla 2026-03-30 181105.png](updates/public/Captura%20de%20pantalla%202026-03-30%20181105.png)

### Arquitectura y Patrones Implementados

1. Patrón MDC (Mapped Diagnostic Context) + Propagación de Headers HTTP (Saga, Usuario) en capa cliente.
2. Observabilidad Desacoplada: Stack de visualización y colección de docker centralizada bajo un entorno separado (`infra/`).


✨ ¡Laboratorios finalizados, sistema listo para escalar!
