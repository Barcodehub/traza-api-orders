# 📊 Update 1.4: Monitoreo Centralizado con Prometheus y Grafana

En esta etapa hemos levantado todo el stack de infraestructura para recolectar y visualizar la telemetría que empezamos a emitir en Update 1.3 de manera local y descentralizada a través de contenedores de Docker.

---

## 🏗️ 1. Infraestructura de Contenedores (`docker-compose.yml`)
Creamos un stack que levanta los servicios base para observabilidad:
- **Prometheus (Puerto 9090):** El motor Time Series Data Base (TSDB) que configuramos para recolectar (`scrape`) las métricas de los microservicios activamente mediante `host.docker.internal` (alojados en los puertos 8081, 8082, y 8083 de la máquina local).
- **Grafana (Puerto 3000):** La interfaz visual donde construiremos los dashboards, configurado para enlazarse con la red de contenedores buscando a Prometheus.
- **Jaeger (Bonus):** Instalamos de forma anticipada la infraestructura del backend de Tracing distribuido, el cual utilizaremos posteriormente al implementar OpenTelemetry en el próximo paso de escalabilidad.

---

## 📡 2. Configuración de Scraping (`prometheus.yml`)
Se definió un archivo dedicado con la misión de buscar y guardar datos.
- **Intervalo de Raspado:** Cada `5s` extrae datos estadísticos para mostrar respuestas casi en tiempo real.
- **Jobs:** Creamos tres instancias estáticas (para `order-service`, `payment-service` e `inventory-service`) apuntándolos al endpoint seguro que habilitamos previamente: `/actuator/prometheus`.

---

## 📈 3. Actualización de Puntos de Control en Negocio
En el código Java del componente principal (`OrderService`), configuramos **Micrometer** para usar una estrategia de nomenclatura basada en el punto (`.`) para que las métricas de negocio importen correctamente y se estructuren en formato OpenMetrics/Prometheus:
- ✅ `orders.created.total` -> *orders_created_total* (Cuenta las Sagas completamente armadas sin errores)
- ❌ `saga.failed.total` -> *saga_failed_total* (Cuenta los retrocesos y compensaciones).

---

## 🎓 4. ¿Cómo utilizar y auditar esto? (Uso a futuro)

1. **Levantar Entorno:** Al ejecutar `docker-compose up -d` y levantar todos los microservicios, el engranaje entrará en funciones.
2. **Revisar Recolector (Prometheus):** 
   - Visita `http://localhost:9090/targets` - Todos los microservicios deberían aparecer en verde (**UP**).
3. **Visibilizar con Grafana (Paneles):**
   - Entrando a `http://localhost:3000` (User/pass: `admin`), conectamos Prometheus como Data Source.
   - Ya podemos programar consultas PromQL reales, ejemplos creados:
     - Promedio de Fallos en Sagas Custom: `rate(saga_failed_total_total[5m])`
     - Tránsito General (Operaciones HTTP): `sum(rate(http_server_requests_seconds_count[1m])) by (application)`
     - Errores de API (5xx) por minuto: `sum(rate(http_server_requests_seconds_count{status="500"}[5m]))`
     - Cálculo matemático de Latencia al 95%: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))`

