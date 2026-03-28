# 📮 Guía Postman - Colección Completa

## 📥 Importar la Colección

### Opción 1: Desde Postman (Interfaz Gráfica)

1. Abre Postman
2. Click en **Import** (botón superior izquierda)
3. Selecciona **Upload Files**
4. Busca y selecciona `postman-collection.json`
5. Click en **Import**

### Opción 2: Arrastrar y Soltar

1. Abre Postman
2. Arrastra el archivo `postman-collection.json` a la ventana de Postman
3. Confirma la importación

---

## 🎯 Variables Automáticas

La colección incluye variables que se configuran automáticamente:

- **order_service_url**: `http://localhost:8081`
- **payment_service_url**: `http://localhost:8082`
- **inventory_service_url**: `http://localhost:8083`
- **order_id**: Se guarda automáticamente al crear una orden
- **saga_id**: Se guarda automáticamente al crear una orden
- **payment_id**: Se guardará automáticamente (si lo necesitas)
- **reservation_id**: Se guardará automáticamente (si lo necesitas)

Puedes editar estas variables en:
- **Collection** → **Variables** (aplica a toda la colección)

---

## 🚀 Flujo Recomendado

### PASO 1: Inicializar

1. Ve a carpeta **"📋 Configuración Inicial"**
2. Ejecuta: **"✅ 1. Disable all failures (Estado limpio)"**
3. Ejecuta: **"✅ 2. Disable inventory failure"**

### PASO 2: Probar Escenario 1 (Exitoso)

1. Abre carpeta **"✅ ESCENARIO 1: Orden Exitosa"**
2. Ejecuta en orden:
   - **"1️⃣ Asegurar que no hay fallos forzados"**
   - **"2️⃣ Crear Orden (debe ser CONFIRMED)"** ← Revisa la respuesta
   - **"3️⃣ Ver Orden Creada"** ← Debe mostrar CONFIRMED

**Resultado esperado:**
```json
{
  "id": "uuid...",
  "userId": "user-scenario1-success",
  "status": "CONFIRMED",
  "total": 150.00,
  "sagaId": "..."
}
```

### PASO 3: Probar Escenario 2 (Fallo Payment)

1. Abre carpeta **"❌ ESCENARIO 2: Fallo en Payment"**
2. Ejecuta en orden:
   - **"1️⃣ Forzar fallo en Payment Service"** ← Activa fallos
   - **"2️⃣ Crear Orden (debe fallar en Payment)"** ← Obtiene 400
   - **"3️⃣ Restaurar Payment Service"** ← Desactiva fallos

**Resultado esperado:** Status 400 con mensaje de error

### PASO 4: Probar Escenario 3 (Compensación)

⚠️ **IMPORTANTE**: Este es el más interesante. Prepara 3 terminales:

**Terminal 1**: Logs de order-service
```bash
tail -f logs/order-service.log | grep SAGA
```

**Terminal 2**: Logs de payment-service
```bash
tail -f logs/payment-service.log | grep SAGA
```

**Terminal 3**: Logs de inventory-service
```bash
tail -f logs/inventory-service.log | grep SAGA
```

**Luego en Postman:**

1. Abre carpeta **"⚙️ ESCENARIO 3: Fallo en Inventory (con Compensación)"**
2. Ejecuta en orden:
   - **"1️⃣ Asegurar Payment OK"**
   - **"2️⃣ Forzar fallo en Inventory Service"** ← Activa fallos
   - **"3️⃣ Crear Orden (fallará en Inventory)"** ← Status 400
   - Mira los LOGS y verás:
     ```
     [SAGA:abc123] Payment SUCCESS
     [SAGA:abc123] Inventory reservation FAILED
     [SAGA:abc123] Compensating: refunding payment  <-- COMPENSACIÓN
     [SAGA:abc123] Payment refunded
     [SAGA:abc123] Order CANCELLED
     ```
   - **"4️⃣ Restaurar Inventory Service"**

---

## 🎲 Pruebas Aleatorias

Abre carpeta **"🎲 Test Aleatorio (sin fallos forzados)"**

Ejecuta:
1. **"1️⃣ Resetear a modo aleatorio"**
2. **"2️⃣ Resetear inventory aleatorio"**
3. **"3️⃣ Crear Orden 1"** - Puede ser ✅ o ❌
4. **"3️⃣ Crear Orden 2"** - Puede ser ✅ o ❌
5. **"3️⃣ Crear Orden 3"** - Puede ser ✅ o ❌

Con probabilidades:
- Payment-service: **30% fallos**
- Inventory-service: **20% fallos**

**Resultado**: Verás una mezcla de órdenes CONFIRMED y CANCELLED

---

## 🛠️ Herramientas (Carpeta "Herramientas")

Requests individuales para debugging:

### Forzar Fallos
- **Enable Payment Failure** - Payment fallará en próximo request
- **Disable Payment Failure** - Payment vuelve a modo aleatorio 30%
- **Enable Inventory Failure** - Inventory fallará en próximo request
- **Disable Inventory Failure** - Inventory vuelve a modo aleatorio 20%

### Consultar
- **Get Order (Last Created)** - Ver última orden creada

---

## 📊 Tests Automáticos

Cada request tiene tests que se ejecutan automáticamente.

**Ver resultados:**
1. Ejecuta un request
2. Ve a pestaña **"Test Results"** (abajo)
3. Verás ✅ o ❌ para cada test

**Ejemplo:**
```
✅ Status es 201
✅ Orden está CONFIRMED
✅ Ordem tiene sagaId
```

---

## 📝 Personalizar Requests

### Cambiar el total de una orden:

1. Abre request **"2️⃣ Crear Orden (debe ser CONFIRMED)"**
2. Ve a pestaña **Body**
3. Modifica el valor de `total`:
   ```json
   {
     "userId": "user-123",
     "total": 999.99
   }
   ```

### Cambiar el usuarioId:

En cualquier request de crear orden, modifica:
```json
{
  "userId": "tu-usuario-personalizado",
  "total": 150.00
}
```

### Cambiar URLs de servicios:

1. Ve a **Collection** → **Variables**
2. Modifica:
   - `order_service_url`
   - `payment_service_url`
   - `inventory_service_url`

---

## 🔍 Debug con Postman

### Ver detalles de una request:

1. Después de ejecutar un request
2. Ve a pestaña **Response**
3. Selecciona formato **Pretty** o **Raw**

### Ver headers:

1. Después de ejecutar un request
2. Ve a pestaña **Headers**
3. Verás todos los headers enviados y recibidos

### Ver Body enviado:

1. En cualquier request
2. Ve a pestaña **Body**
3. Verás el JSON que se envía

---

## 💡 Tips Útiles

### Ejecutar una carpeta completa

1. Click derecho en una carpeta (ej: "ESCENARIO 1")
2. Selecciona **"Run"**
3. Postman ejecutará todos los requests en orden

### Ver historial de requests:

1. Abre **History** (izquierda)
2. Verás todos los requests ejecutados
3. Puedes hacer click para volver a ejecutar

### Copiar un request:

1. Click derecho en un request
2. **Duplicate**
3. Edita la copia

### Renombrar un request:

1. Click derecho
2. **Rename**
3. Ingresa nuevo nombre

---

## 🐛 Troubleshooting

### Error: "Could not get any response"

**Problema**: Los servicios no están corriendo

**Solución**:
```bash
./start-all.sh    # Linux/Mac
start-all.bat     # Windows
```

### Error: "Status 400" inesperado

**Verificar:**
1. ¿Todos los 3 servicios están corriendo?
2. Revisa los logs:
   ```
   [SAGA:xxx] Error occurred
   ```

### Las variables no se guardan

1. Ve a **Collection** → **Variables**
2. Asegúrate de que exista la variable
3. Ejecuta un request que guarde la variable (ej: crear orden)

### No veo los Test Results

1. Ejecuta el request
2. Scrollea hacia abajo en la ventana derecha
3. Haz click en pestaña **Test Results**

---

## 📚 Documentación Completa

- **README.md** - Documentación general
- **QUICKSTART.md** - Inicio rápido
- **CHECKLIST.md** - Lista de verificación
- **DIAGRAMS.md** - Diagramas de flujo

---

## 🎉 Listo para empezar

1. Importa la colección
2. Asegúrate de que los servicios estén corriendo
3. Sigue el "Flujo Recomendado" arriba
4. ¡Disfruta explorando los microservicios!

---

**¿Preguntas?** Revisa los logs en las terminales de cada servicio para ver qué está pasando con cada request.
