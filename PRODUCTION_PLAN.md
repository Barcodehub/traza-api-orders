# Plan de migración a Producción con Azure DevOps, Terraform y Kubernetes

Para llevar tu arquitectura de microservicios (`inventory-service`, `order-service`, `payment-service`) a un entorno de producción de manera automatizada y profesional, el enfoque cambia del entorno local (donde usas `docker-compose.yml`) a un modelo distribuido, escalable y gestionado mediante **Infraestructura como Código (IaC)** y **Pipelines CI/CD**.

Aquí te explico detalladamente qué cambia y cómo se estructura la nueva arquitectura:

## 1. El rol del `docker-compose.yml`
El archivo `docker-compose.yml` **no se elimina**, pero ya no se usa para producción. Se mantiene exclusivamente para el **entorno de desarrollo local**. Permite a los desarrolladores levantar toda la pila (servicios, Kafka, Datadog) en sus máquinas con un solo comando.

## 2. Nueva Arquitectura y Herramientas

### A. Terraform (Infraestructura como Código - IaC)
En lugar de crear servidores a mano, utilizarás Terraform para provisionar los recursos en Azure automáticamente:
- **AKS (Azure Kubernetes Service):** El clúster donde se ejecutarán tus contenedores.
- **ACR (Azure Container Registry):** El registro privado donde se guardarán las imágenes Docker de tus microservicios (reemplaza a Docker Hub).
- **Recursos adicionales:** Virtual Networks, bases de datos gestionadas (si las extraes de los contenedores), y Key Vault para guardar secretos.

*Crearás una carpeta `terraform/` con archivos `.tf` (main, variables, outputs).*

### B. Kubernetes (Manifiestos o Helm Charts)
El `docker-compose.yml` se traduce a **Manifiestos de Kubernetes (YAML)**. Cada servicio de tu compose pasa a ser:
- **Deployment:** Para definir cuántas réplicas del contenedor quieres, la imagen (desde ACR), variables de entorno y recursos (CPU/RAM).
- **Service:** Para comunicar los microservicios entre sí.
- **ConfigMap/Secret:** Para manejar las variables de entorno (como `SPRING_KAFKA_BOOTSTRAP_SERVERS` o `DD_API_KEY`).
- **Ingress:** Para exponer tus servicios al mundo exterior (rutas HTTP).

*Crearás una carpeta `k8s/` con estos manifiestos.*

### C. Azure DevOps (Pipelines CI/CD)
El proceso de compilación y despliegue se automatiza al 100% mediante pipelines (un archivo `azure-pipelines.yml` en la raíz del proyecto). 
El pipeline típicamente tiene dos etapas (Stages):
1. **CI (Continuous Integration):**
   - Compila el código Java (Gradle).
   - Ejecuta pruebas unitarias e integración.
   - Construye la imagen Docker (`docker build`).
   - Sube la imagen Docker al ACR con un tag único (por ejemplo, el ID del commit).
2. **CD (Continuous Deployment):**
   - Descarga los manifiestos de Kubernetes.
   - Actualiza la imagen en los manifiestos con el nuevo tag.
   - Aplica los manifiestos al clúster AKS (`kubectl apply`).

## 3. ¿Qué pasa con las dependencias (Kafka y Datadog)?
En producción no sueles correr bases de datos o sistemas de mensajería (Kafka) dentro del mismo clúster de Kubernetes usando contenedores básicos como en Docker Compose, por temas de persistencia y rendimiento.
- **Kafka:** Se suele delegar a un servicio gestionado (como Confluent Cloud, Azure Event Hubs con compatibilidad Kafka, o instalado vía Strimzi Operator en AKS).
- **Datadog:** En lugar de un contenedor normal, se despliega como un **DaemonSet** en Kubernetes, lo que asegura que haya un agente de Datadog ejecutándose en cada nodo (servidor físico/virtual) del clúster AKS recogiendo métricas, logs y trazas APM.

## Pasos para implementar esto (Roadmap)

1. **Crear el código de Terraform:** Proveer la infraestructura base en Azure (Resource Group, ACR y AKS).
2. **Crear Manifiestos K8s (`k8s/`):** Traducir los servicios del compose a `deployment.yaml` y `service.yaml`.
3. **Configurar Azure DevOps:** 
   - Conectar Azure Repos/GitHub con Azure Pipelines.
   - Crear el archivo `azure-pipelines.yml` para empaquetar y desplegar.
4. **Configurar Secretos:** Manejar el `DD_API_KEY` o credenciales de forma segura usando Azure Key Vault conectado a AKS.

## Estructura de carpetas sugerida para tu proyecto (Nuevo):
```text
products-management/
 ├── .azure/
 │    └── azure-pipelines.yml      <-- Automatización CI/CD
 ├── terraform/                    <-- Creación de la nube (AKS, ACR)
 │    ├── main.tf
 │    ├── variables.tf
 │    └── providers.tf
 ├── k8s/                          <-- Cómo ejecutar los contenedores en AKS
 │    ├── inventory-service.yaml
 │    ├── order-service.yaml
 │    └── payment-service.yaml
 ├── infra/
 │    └── docker-compose.yml       <-- Queda solo para desarrollo local
 ├── inventory-service/
 ├── order-service/
 └── payment-service/
```
