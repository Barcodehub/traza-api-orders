variable "location" {
  description = "Región de Azure (ej. eastus2)"
  type        = string
  default     = "eastus2"
}

variable "environment" {
  description = "Entorno (prod, dev, stg)"
  type        = string
  default     = "prod"
}

variable "project" {
  description = "Nombre del proyecto"
  type        = string
  default     = "bankprodmgt"
}

variable "aks_node_count" {
  description = "Número mínimo de nodos en AKS"
  type        = number
  default     = 2
}

variable "aks_vm_size" {
  description = "Tamaño de las VMs de AKS"
  type        = string
  default     = "Standard_B2s"
}
