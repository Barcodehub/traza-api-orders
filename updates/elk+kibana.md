# Implementación de Centralización de Logs con ELK Stack (Elasticsearch + Kibana + Filebeat)

Durante esta fase, escalamos nuestra arquitectura de observabilidad agregando un stack centralizado de logs para todos los microservicios.

## ¿Qué hicimos?
1. **Configuración de ELK en Docker-Compose:** Agregamos `elasticsearch`, `kibana` y `filebeat` al entorno gestionado por Docker Compose bajo la carpeta `infra/`.
2. **Uso de Filebeat:** Configuramos Filebeat como agente recolector para que tome los archivos de log `.log` (generados por los microservicios) y los envíe automáticamente a Elasticsearch.
3. **Mapeo de Volúmenes (El problema de la lista de índices vacía corregido):** Los logs actualmente se están alojando en la carpeta raíz `logs/` de manera consolidada solo para pruebas fáciles en desarrollo. Filebeat se conectó usando el volumen `../logs:/usr/share/logs:ro` y el path de los inputs actualizado a `paths: - /usr/share/logs/*.log`.
4. **Índices en Elasticsearch:** Configuramos a Filebeat para que enviara registros en formato JSON (`ndjson`) bajo el patrón `microservices-logs-*`.

## Aclaración sobre estructura de carpetas
* **Directorio de logs compartido:** Al tener 3 microservicios independientes, la mejor opción si los levantamos nativos (en localhost sin dockerizar y sin herramientas para recolectar stdout) era centralizar el archivo de texto en una misma carpeta (`logs/`).
* **Directorio `infra/` para configuraciones compartidas:** Las herramientas de observabilidad (Prometheus, ELK, Grafana) no pertenecen al código de negocio de un único microservicio. Por eso están en `infra/`.

## ¿Cómo sería si Dockerizamos los microservicios? (El salto en el futuro)
Si los microservicios también se convierten en contenedores gestionados por Docker Compose, la gestión de logs cambia y mejora:
1. **Cero Archivos:** Los microservicios dejarían de escribir logs a archivos `.log` usando Logback, solo emitirían los JSON a `Console` (`stdout`/`stderr`).
2. **Docker Logging Drivers:** Retiraríamos el volumen y configuraríamos Docker para capturar el `stdout` de **cada microservicio** independientemente. Filebeat puede interactuar directamente con el runtime de Docker por socket para enviarlos a Kibana. 
3. **Escalabilidad y buenas prácticas (12-Factor App):** Así evitamos llenar el disco por los logs y logramos que los microservicios sean *Stateless* al 100%. Las herramientas de infraestructura se separarían completamente, en otro repositorio o gestionadas aisladamente.
