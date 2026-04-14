Antes de correr tu código de Terraform debes decirle a tu nube "activa mis permisos para usar Kubernetes, registros Docker y la bóveda de llaves".

az provider register --namespace Microsoft.ContainerService
az provider register --namespace Microsoft.ContainerRegistry
az provider register --namespace Microsoft.DevOps
az provider register --namespace Microsoft.KeyVault

cd terraform
Y ejecuta su ciclo de vida (estas son las instrucciones que creamos):
terraform init (Trozzo de código que descargará los módulos de Azure HashiCorp que declaramos en providers.tf).
terraform plan -out mibanco.tfplan (Este comando simulará todo: leerá tus variables y te mostrará el plan con signos + verdes de todo lo que va a crear en tu nueva cuenta para que estés seguro).
terraform apply "mibanco.tfplan" (Este comando le da la luz verde y comenzará a comunicarse directamente con Microsoft Azure para crear tu clúster AKS, el ACR y el Key Vault).