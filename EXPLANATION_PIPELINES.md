# Guía para Principiantes: Azure DevOps (Pipelines CI/CD)

Imagina que estás construyendo autos de forma artesanal. Cada vez que terminas uno, debes caminar hasta el taller, pintarlo a mano, revisar el motor y llevarlo conduciendo hasta el cliente. Esto toma horas y puedes equivocarte en el proceso.

Un **Pipeline (Tubería)** es una **línea de ensamblaje en una fábrica automatizada**. Cada vez que un desarrollador escribe una nueva línea de código y la guarda en la rama principal (`main`), la fábrica se enciende sola. Escanea el código, lo prueba, lo construye y lo instala en los servidores de producción. 

Todo esto ocurre sin intervención humana. A esto se le conoce como **CI/CD** (Integración Continua / Despliegue Continuo).

## ¿Cómo leer el archivo `.yml` del pipeline?

El archivo (`ej: inventory-pipeline.yml`) define paso a paso todo lo que debe ocurrir en esta "fábrica".

### 1. ¿Qué activa la fábrica? (`trigger`)
```yaml
trigger:
  branches:
    include:
      - main
  paths:
    include:
      - 'inventory-service/*'
```
Esto dice: "Enciende la fábrica **solamente** cuando alguien guarde cambios en la rama `main` y esos cambios estén adentro de la carpeta `inventory-service`". Así evitamos compilar todo el proyecto si solo modificamos el inventario.

### 2. Etapa 1: Construcción (Stage: Build)
Esta es la primera parte de la fábrica. Azure pide una computadora gratuita prestada (`ubuntu-latest`) temporalmente.
Dentro de esta computadora se ejecutan pasos secuenciales:
1. **Tarea de Gradle (`task: Gradle@3`)**: Ensambla el código de Java, ejecuta las pruebas (para asegurar que nada se rompió) y hace un análisis de seguridad del código (SonarQube) para detectar bugs de seguridad.
2. **Tarea de Docker (`task: Docker@2`)**: Toma el código Java ya ensamblado y lo empaqueta en una "caja" (Contenedor). Luego toma esa caja y la envía (Push) a nuestra bodega de cajas (nuestro Container Registry privado en Azure, configurado previamente por Terraform). Cada caja se etiqueta con el ID de la ejecución (ejemplo: `imagen_v123`) para que jamás se sobrescriba.

### 3. Etapa 2: Despliegue (Stage: Deploy)
Esta etapa **depende de que la Etapa 1 haya salido perfecta**. Si el código no pasó las pruebas de seguridad, la fábrica se detiene y avisa por email. Si todo salió bien:
1. **Tarea de Kubernetes (`task: KubernetesManifest@1`)**: Azure Pipeline toma la llave maestra y habla en privado con el clúster de Kubernetes en producción. 
2. Le dice a Kubernetes: "Aquí tienes los archivos de configuración actualizados (`deployment.yaml` y `service.yaml`). Por favor, diles a las máquinas actuales que apaguen las cajas viejas gradualmente y comiencen a usar esta nueva caja versión 123 que acaban de construir".

¡Y listo! Tu código pasó de estar en tu computador, a estar corriendo en los servidores de la alta gerencia bancaria mundial de forma totalmente automática, probada y sin cortes en la aplicación activa.

