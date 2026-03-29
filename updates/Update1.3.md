# 🚀 Update 1.3: Observabilidad y Métricas con Prometheus

En esta fase, convertimos nuestros microservicios de "cajas negras" a sistemas totalmente monitoreables. Implementamos **Spring Boot Actuator**, **Micrometer** y **Prometheus** para obtener telemetría en tiempo real sobre la salud y rendimiento del negocio.

---

## 🛠️ 1. Nuevas Herramientas (Dependencias)
Añadimos dos librerías clave a todos nuestros microservicios (`order`, `payment`, `inventory`):
* **`spring-boot-starter-actuator`**: Expone la salud del sistema y detecta métricas base automáticamente (CPU, memoria, tráfico HTTP).
* **`micrometer-registry-prometheus`**: Traduce esas métricas de Java al formato de texto estricto que requiere Prometheus para leerlas.

---

## ⚙️ 2. Configuración Segura (`application.yml`)
Modificamos los archivos de propiedades para habilitar la recopilación de datos sin comprometer la seguridad:

* 🔒 **Endpoints limitados:** Solo abrimos `/actuator/health` y `/actuator/prometheus` (no exponemos base de datos ni variables de entorno).
* 🏷️ **Etiquetado Inteligente:** Inyectamos automáticamente el tag `application: <nombre-servicio>` a cada métrica, vital para filtrar por microservicio en Grafana.
* ⏱️ **Medición de Latencias:** Habilitamos `percentiles-histogram` para calcular los tiempos de respuesta HTTP exactos (Percentiles 95, 99).

---

## 📈 3. Métricas de Negocio (Custom Metrics)
Además de las métricas técnicas, enseñamos al código a reportar cómo le va al negocio. En `OrderService.java` registramos dos contadores (`Counters`):

1. ✅ **`business.orders.created.total`**: Suma +1 cada vez que un pedido completa su Saga exitosamente.
2. ❌ **`business.sagas.failed.total`**: Suma +1 cada vez que falla una reserva o pago y se lanza una compensación (rollback).

---

## 🔎 4. Ejemplos de Consultas (PromQL)
Gracias a estos cambios, podemos extraer gráficas de valor preguntándole a Prometheus. Ejemplos de código **PromQL**:

* **¿Cuántos errores de Saga tenemos por segundo?**
  ```promql
  rate(business_sagas_failed_total[5m])
  ```
* **¿Cuál es la latencia (P95) de nuestras APIs?**
  ```promql
  histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri!="/actuator/prometheus"}[5m])) by (le, application))
  ```
* **¿Cuántas peticiones en total ha recibido un servicio?**
  ```promql
  sum(http_server_requests_seconds_count{application="order-service"})
  ```
