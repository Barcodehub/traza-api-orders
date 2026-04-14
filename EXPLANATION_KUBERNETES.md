# Guía para Principiantes: Entendiendo Kubernetes

Kubernetes (a menudo abreviado como **K8s**) es como un **director de orquesta** para tus aplicaciones. 

Tu aplicación (por ejemplo, `inventory-service`) está empaquetada en una "caja" llamada **Contenedor** (usualmente usando Docker). Un contenedor tiene el programa de Java, el sistema operativo básico y todo lo que necesita para correr. 

¿Pero qué pasa si esa caja se apaga por un error? ¿O si hay tantos usuarios que necesitas 10 copias de la misma caja trabajando al mismo tiempo? **Kubernetes es quien se encarga de vigilar, multiplicar y mantener vivas esas cajas.**

## Conceptos Básicos de los Archivos YAML de Kubernetes

En Kubernetes no programas con código como Java o Python, le pasas archivos de texto plano (manifiestos) escritos en un formato llamado **YAML**, donde declaras el "estado deseado" (ejemplo: "Quiero 2 cajas funcionando siempre"). Kubernetes hace lo posible por cumplir tus deseos.

En nuestro proyecto bancario creamos 2 archivos por cada microservicio (`deployment.yaml` y `service.yaml`).

### 1. El archivo `deployment.yaml` (El Gestor)
Este archivo define **CÓMO** y **CUÁNTAS** instancias de tu aplicación van a existir.

* **`replicas: 2`**: Le dice a K8s que siempre debe haber 2 contenedores de este servicio funcionando. Si uno colapsa, K8s crea otro automáticamente.
* **`image: acrbankprodmgt...`**: Le indica al clúster de dónde descargar la "caja" de la aplicación (la saca de nuestro ACR privado de Azure).
* **`securityContext`**: Aquí están las **reglas de seguridad bancaria**. Le decimos que el programa NO puede correr con permisos de Administrador (`runAsNonRoot: true`), no puede modificar los archivos del sistema operativo interno (`readOnlyRootFilesystem: true`), y le quitamos todos los privilegios a nivel de kernel (`capabilities: drop: ALL`). ¡A prueba de hackers!
* **`resources` (Limites de CPU/RAM)**: Evita que el programa se vuelva loco y consuma toda la memoria del servidor (`limits: memory: 512Mi`). Garantiza orden.
* **`livenessProbe` / `readinessProbe` (Sondas de vida):** Kubernetes es ciego por defecto. Con estas reglas, K8s hace una petición "ping" al puerto 8080 cada 10 segundos. Si el microservicio no responde, K8s sabe que se "murió" y reinicia la caja.

### 2. El archivo `service.yaml` (El Directorio Telefónico)
Los contenedores nacen y mueren todo el tiempo, y cada vez que nacen, reciben una IP diferente. Sería imposible que un microservicio se comunique con otro porque las IPs cambian a cada rato.

El **Service** soluciona esto aportando un nombre fijo. 
En este archivo decimos: "Todo el tráfico que llegue al nombre `inventory-service` en el puerto 80, envíalo a cualquiera de las copias que estén vivas en ese momento en el puerto 8080".

Así, tu aplicación `order-service` solo necesita buscar a `inventory-service` por su nombre y Kubernetes se encarga de rutear la comunicación (hace de balanceador de carga interno).

