# Preparación para la Entrevista (Pitches Técnicos Explicados a Detalle)

Aquí tienes cómo responder de manera que se entienda **qué significa cada cosa que dices**, explicando el "por qué" y el "para qué" en un lenguaje claro pero profesional.

---

### 1. ¿Cómo implementas Terraform?

*Lo que debes decir para que quede súper claro:*
> "En lugar de entrar a la página web de Azure y hacer clics para crear servidores manualmente (lo cual trae errores humanos y no deja historial), yo escribo todo como código usando Terraform. 
> 
> Para organizarme, separo el código en varios archivos físicos. Por ejemplo:
> - **El archivo `variables.tf`**: Aquí defino valores que pueden cambiar. Por ejemplo, escribo 'tamaño_de_maquina = 4GB'. Si mañana el banco me pide cambiar esto a 8GB, solo cambio esa línea en este archivo y automáticamente todo el proyecto toma el nuevo valor. A esto me refiero con que me permite 'flexibilizar entornos'.
> - **Archivos dedicados como `aks.tf`**: Aquí es donde escribo literalmente el bloque de código que le ordena a Azure 'Créame un clúster de Kubernetes'. Adentro de este bloque, le pongo reglas de seguridad escritas, como `private_cluster_enabled = true` para que todo mi sistema quede oculto del Internet público y sea completamente privado.
> - Y para que todo esto no ocupe contraseñas (que se pueden robar), le asgino lo que se llama una 'Identidad Manejada' (`SystemAssigned`); es decir, Azure le da un permiso especial a mi clúster para que por sí solo pueda descargar cosas de mi bóveda privada sin usar contraseñas.
> 
> Finalmente, cuando termino de escribir los archivos, abro la consola y ejecuto 3 comandos: `terraform init` para que el programa se prepare, `terraform plan` que me muestra una simulación en pantalla de lo que va a crear (para asegurarme de que no voy a borrar nada importante del banco), y `terraform apply` que es el botón de 'Hacerlo realidad en la nube'."

---

### 2. ¿Cómo implementas tú Kubernetes?

*Lo que debes decir para que quede súper claro:*
> "Para Kubernetes no uso comandos manuales; escribo archivos de texto en formato YAML que le dicen a la herramienta cómo quiero que funcionen mis aplicaciones. Para cada microservicio, como el de Inventario, escribo dos archivos principales:
> 
> 1. El **`deployment.yaml`**: Aquí le defino a Kubernetes cuántas copias de mi aplicación quiero tener vivas al mismo tiempo, como mínimo dos. Además, le meto fuertes reglas de seguridad dentro de un bloque llamado `securityContext`. Ahí le pongo `runAsNonRoot: true`, que significa que la aplicación no puede correr como Administrador (así un hacker no puede tomar el control del servidor base). También le pongo `readOnlyRootFilesystem`, que bloquea físicamente el disco del contenedor para que nadie pueda instalar un virus de minería o borrar archivos mientras la app está encendida.
> Además, en este mismo archivo, le escribo una regla llamada `livenessProbe`. Esto funciona como un 'doctor': hace que Kubernetes le esté preguntando a la ruta `.../actuator/health` de mi microservicio Java cada 10 segundos cómo está. Si la aplicación de Java se bloquea o deja de responder, Kubernetes la borra y crea una nueva sola, sin despertarme en la madrugada.
> 
> 2. El **`service.yaml`**: Como Kubernetes reinicia aplicaciones y les cambia el número IP todo el tiempo, este archivo crea un nombre fijo (como un directorio telefónico). Así, el microservicio de Órdenes siempre sabrá cómo llamar al de Inventario por su nombre, sin importar cuál es su IP en ese momento."

---

### 3. ¿Cómo implementas los Pipelines con Azure DevOps?

*Lo que debes decir para que quede súper claro:*
> "Para automatizar que mis programas pasen de mi computador a producción, escribo un archivo llamado `azure-pipelines.yml`. Es básicamente la receta paso a paso de lo que debe hacer la empresa.
> 
> Lo primero que configuro es el disparador (`trigger`). Como tengo mis 3 microservicios en una sola carpeta general (monorepo), le ordeno a Azure DevOps: 'Si el programador modificó un archivo, revisa en qué carpeta fue (con la regla `paths: include`). Si el cambio fue en la carpeta de Inventario, arranca solo el proceso de Inventario y no me toques los Pagos'. Esto ahorra mucho tiempo.
> 
> Después, divido el pipeline en dos pasos grandes (Stages):
> - **El paso de Construcción (Build):** Aquí le digo al pipeline que tome una computadora de Linux prestada (Ubuntu), descargue mi código, ejecute la compilación normal de Gradle, y lo revise con SonarQube (un antivirus de código). Luego le digo que lo empaquete en un contenedor de Docker y le etiquete el número de ejecución; por ejemplo, 'inventario:version_50'. Esto significa que siempre sabré de qué fecha y código vino esa versión.
> - **El paso de Despliegue (Deploy):** Si todo lo anterior pasó sin errores, comienza esta etapa. Se conecta de forma segura y privada al entorno de producción (al clúster de Kubernetes) y le pasa mis archivos `.yaml`. Y mágicamente, Kubernetes empieza a cambiar las máquinas viejas por esta nueva 'versión 50' poco a poco para que el banco nunca se caiga ni un segundo."
