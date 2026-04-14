# Guía para Principiantes: Infraestructura como Código (Terraform)

Si nunca has usado Terraform, imagínalo como **un plano arquitectónico virtual**. En lugar de entrar a la página web de Azure, hacer clic en botones y crear servidores manualmente (lo cual es lento y propenso a errores humanos), escribimos un archivo de texto con las "instrucciones" de lo que queremos. Terraform lee ese archivo y construye todo automáticamente en la nube.

A esto se le llama **Infraestructura como Código (IaC)**.

## Conceptos Básicos

- **Provider (Proveedor):** Es el plugin que conecta Terraform con la nube elegida. En nuestro caso, el proveedor es `azurerm` (Azure Resource Manager). Le dice a Terraform "vamos a crear cosas en Microsoft Azure".
- **Resource (Recurso):** Es cualquier pieza de infraestructura en la nube. Puede ser una base de datos, un servidor, una red, etc.
- **Variable:** Es un valor que se puede cambiar fácilmente sin alterar el código principal. Por ejemplo, el nombre del proyecto o la región (ubicación de los servidores).

## ¿Qué hacen nuestros archivos `.tf`?

1. **`providers.tf`:** Aquí le decimos a Terraform que descargue las herramientas necesarias para hablar con Azure. Es como el "traductor" entre nuestro código y la nube.
2. **`variables.tf`:** Aquí definimos "etiquetas" reutilizables. Por ejemplo, definimos que el tamaño de las máquinas virtuales será `Standard_D4s_v3`. Si en el futuro queremos máquinas más grandes, solo cambiamos este archivo.
3. **`main.tf`:** 
   - Crea un **Resource Group** (Grupo de Recursos): Imagina esto como una carpeta principal en Azure donde se guardará todo nuestro proyecto.
   - Crea una **Virtual Network** (Red Virtual): Es como el cableado Wi-Fi privado para que nuestros servidores se comuniquen sin estar expuestos a Internet.
4. **`acr.tf`:** Crea un **Azure Container Registry (ACR)**. Imagina que es un "Google Drive privado" pero diseñado específicamente para almacenar las imágenes (paquetes) de nuestras aplicaciones (microservicios).
5. **`aks.tf`:** Crea el **Azure Kubernetes Service (AKS)**. Este es el "sistema operativo" de la nube que va a ejecutar y administrar nuestros programas. Lo configuramos **totalmente privado** y con políticas de auto-escalado (si hay más clientes en el banco, el clúster enciende computadoras automáticamente; si no hay nadie, las apaga).
6. **`keyvault.tf`:** Crea un **Key Vault** (Bóveda de llaves). Es una caja fuerte ultra-segura para guardar contraseñas, certificados y tokens. Solo los sistemas autorizados pueden abrirla, ni siquiera los desarrolladores ven las claves de producción.

## ¿Cómo se usa?
Si quisieras ejecutar esto tú mismo en tu terminal, solo necesitas usar 3 comandos:
1. `terraform init` (Prepara todo e instala el proveedor de Azure).
2. `terraform plan` (Te muestra un resumen de "Voy a crear 15 recursos nuevos", sin hacer cambios reales).
3. `terraform apply` (Construye todo en la nube).

