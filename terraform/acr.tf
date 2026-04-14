resource "azurerm_container_registry" "acr" {
  name                          = "acr${var.project}${var.environment}"
  resource_group_name           = azurerm_resource_group.rg.name
  location                      = azurerm_resource_group.rg.location
  sku                           = "Standard"
  admin_enabled                 = false
  public_network_access_enabled = false

  tags = {
    Environment = var.environment
    Compliance  = "PCI-DSS"
  }
}

