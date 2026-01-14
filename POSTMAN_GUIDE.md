# 📮 Guía de Postman - Digital Money House API

## 🚀 Importar la Colección

1. Abre **Postman**
2. Click en **Import** (botón en la esquina superior izquierda)
3. Arrastra el archivo `Digital_Money_House_API.postman_collection.json` o selecciónalo
4. Click en **Import**

¡Listo! Ahora tendrás todos los 22 endpoints organizados en 3 carpetas.

---

## 📂 Estructura de la Colección

```
Digital Money House API
├── User Service (6 endpoints)
│   ├── 1. Register User
│   ├── 2. Login
│   ├── 3. Logout
│   ├── 4. Validate Token
│   ├── 5. Get User by ID
│   └── 6. Update User
│
├── Account Service (12 endpoints)
│   ├── 7. Create Account
│   ├── 8. Get Account by User ID
│   ├── 9. Get Account by ID
│   ├── 10. Get Last Transactions
│   ├── 11. Get All Activity (No Filters)
│   ├── 12. Filter Activity by Type
│   ├── 13. Filter Activity by Amount Range
│   ├── 14. Filter Activity by Date Period
│   ├── 15. Filter Activity (Multiple Filters)
│   ├── 16. Get Activity Detail
│   ├── 17. Deposit from Card
│   └── 18. Update Account Alias
│
└── Card Service (4 endpoints)
    ├── 19. Get All Cards
    ├── 20. Get Card by ID
    ├── 21. Create Card
    └── 22. Delete Card (Block)
```

---

## 🔧 Variables de Colección

La colección incluye estas variables que se actualizan automáticamente:

| Variable | Descripción | Valor Inicial |
|----------|-------------|---------------|
| `baseUrl` | URL base del API Gateway | `http://localhost:8080` |
| `token` | JWT token (se guarda al hacer login) | vacío |
| `userId` | ID del usuario actual | `1` |
| `accountId` | ID de la cuenta actual | `1` |
| `cardId` | ID de la última tarjeta creada | `1` |
| `transactionId` | ID de la última transacción | `1` |

### Ver/Editar Variables

1. Click en la colección **Digital Money House API**
2. Pestaña **Variables**
3. Modifica los valores según necesites

---

## 🎯 Flujo de Prueba Recomendado

### Paso 1: Configuración Inicial

1. **Inicia los servicios** (en orden):
   ```bash
   # Terminal 1 - Config Server
   cd config-server && mvn spring-boot:run
   
   # Terminal 2 - Eureka Server
   cd eureka-server && mvn spring-boot:run
   
   # Terminal 3 - User Service
   cd user-service && mvn spring-boot:run
   
   # Terminal 4 - Account Service
   cd account-service && mvn spring-boot:run
   
   # Terminal 5 - API Gateway
   cd api-gateway && mvn spring-boot:run
   ```

2. **Verifica que todos estén activos**:
   - Config Server: http://localhost:8888
   - Eureka Dashboard: http://localhost:8761
   - API Gateway: http://localhost:8080

### Paso 2: Registro y Autenticación

1. **Ejecuta "1. Register User"**
   - Se crea el usuario y su cuenta automáticamente
   - Las variables `userId` y `accountId` se guardan automáticamente

2. **Ejecuta "2. Login"**
   - El `token` se guarda automáticamente en las variables
   - Este token se usará para todos los endpoints protegidos

3. **Ejecuta "4. Validate Token"** (opcional)
   - Verifica que el token sea válido

### Paso 3: Gestión de Perfil

4. **Ejecuta "5. Get User by ID"**
   - Obtiene tu perfil completo

5. **Ejecuta "6. Update User"** (opcional)
   - Modifica tu nombre o teléfono

### Paso 4: Gestión de Cuenta

6. **Ejecuta "8. Get Account by User ID"**
   - Verifica tu cuenta y balance inicial (0.00)

7. **Ejecuta "18. Update Account Alias"** (opcional)
   - Personaliza el alias de tu billetera

### Paso 5: Gestión de Tarjetas

8. **Ejecuta "21. Create Card"**
   - Crea una tarjeta de débito o crédito
   - El `cardId` se guarda automáticamente

9. **Ejecuta "19. Get All Cards"**
   - Verifica que tu tarjeta se creó correctamente

10. **Ejecuta "20. Get Card by ID"**
    - Obtiene detalles de una tarjeta específica

### Paso 6: Ingresar Dinero

11. **Ejecuta "17. Deposit from Card"**
    - Ingresa $500 desde tu tarjeta
    - El balance se actualiza automáticamente
    - El `transactionId` se guarda automáticamente

### Paso 7: Consultar Actividad

12. **Ejecuta "11. Get All Activity (No Filters)"**
    - Ve todas tus transacciones

13. **Ejecuta "16. Get Activity Detail"**
    - Obtiene el detalle de una transacción específica

14. **Prueba los filtros**:
    - **"12. Filter Activity by Type"**: Solo depósitos o retiros
    - **"13. Filter Activity by Amount Range"**: Por rangos de monto
    - **"14. Filter Activity by Date Period"**: Por fechas
    - **"15. Filter Activity (Multiple Filters)"**: Combinación de filtros

### Paso 8: Transacciones Adicionales

15. **Ejecuta "17. Deposit from Card"** varias veces
    - Con diferentes montos para probar los filtros
    - Ejemplos: $300, $2500, $15000, $150000

16. **Ejecuta "10. Get Last Transactions"**
    - Ve las últimas 10 transacciones

### Paso 9: Gestión de Tarjetas (Cleanup)

17. **Ejecuta "22. Delete Card (Block)"**
    - Bloquea una tarjeta (soft delete)

18. **Ejecuta "3. Logout"**
    - Cierra la sesión

---

## 🎨 Scripts Automáticos

Los siguientes endpoints incluyen scripts que actualizan variables automáticamente:

### 1. Register User
```javascript
// Guarda userId y accountId
pm.collectionVariables.set('userId', response.id);
pm.collectionVariables.set('accountId', response.accountId);
```

### 2. Login
```javascript
// Guarda el token JWT
pm.collectionVariables.set('token', response.token);
pm.collectionVariables.set('userId', response.userId);
```

### 17. Deposit from Card
```javascript
// Guarda transactionId y muestra el nuevo balance
pm.collectionVariables.set('transactionId', response.transactionId);
console.log('New balance: ' + response.newBalance);
```

### 21. Create Card
```javascript
// Guarda cardId
pm.collectionVariables.set('cardId', response.id);
```

---

## 📊 Ejemplos de Uso

### Filtrar Depósitos de Más de $100,000 en Enero 2026

```
GET {{baseUrl}}/api/accounts/{{accountId}}/activity?type=DEPOSIT&amountRange=RANGE_OVER_100000&dateFrom=2026-01-01&dateTo=2026-01-31
Authorization: Bearer {{token}}
```

### Obtener Todas las Transacciones entre $1,000 y $5,000

```
GET {{baseUrl}}/api/accounts/{{accountId}}/activity?amountRange=RANGE_1000_5000
Authorization: Bearer {{token}}
```

### Ver Solo Retiros de los Últimos 7 Días

```
GET {{baseUrl}}/api/accounts/{{accountId}}/activity?type=WITHDRAWAL&dateFrom=2026-01-07
Authorization: Bearer {{token}}
```

---

## 🔐 Autenticación

### Endpoints Públicos (sin token):
- Register User
- Login
- Validate Token
- Create Account
- Get Account by User ID
- Get Account by ID
- Get Last Transactions

### Endpoints Protegidos (requieren token):
- Logout
- Get User by ID
- Update User
- Get All Activity + Filtros
- Get Activity Detail
- Deposit from Card
- Todos los endpoints de Cards

---

## ⚠️ Notas Importantes

### Orden de Ejecución
1. **Siempre ejecuta "Login" antes** de los endpoints protegidos
2. **Crea una tarjeta antes** de hacer un depósito
3. **Haz algunos depósitos antes** de probar los filtros

### Variables Dinámicas
- Los IDs se actualizan automáticamente tras crear recursos
- Puedes editar manualmente las variables si necesitas probar con IDs específicos

### Múltiples Usuarios
Si quieres probar con múltiples usuarios:
1. Cambia el email en "Register User"
2. Ejecuta el flujo completo con el nuevo usuario
3. Usa diferentes colecciones o entornos en Postman

### Errores Comunes

| Error | Causa | Solución |
|-------|-------|----------|
| 401 Unauthorized | Token inválido/expirado | Ejecuta "Login" nuevamente |
| 403 Forbidden | Intentas acceder a recursos de otro usuario | Verifica el `userId` |
| 404 Not Found | ID incorrecto | Verifica las variables `accountId`, `cardId`, etc. |
| 400 Bad Request | Datos inválidos en el body | Revisa el formato JSON |

---

## 🧪 Testing Avanzado

### Usar Entornos (Environments)

Puedes crear entornos para diferentes ambientes:

**Local**:
```json
{
  "baseUrl": "http://localhost:8080"
}
```

**Development**:
```json
{
  "baseUrl": "https://dev-api.digitalmoney.com"
}
```

**Production**:
```json
{
  "baseUrl": "https://api.digitalmoney.com"
}
```

### Runner de Colección

Para ejecutar todos los endpoints en secuencia:
1. Click derecho en la colección
2. **Run collection**
3. Configura el delay entre requests (ej: 500ms)
4. Click **Run Digital Money House API**

---

## 📝 Logs en Consola

Los scripts automáticos muestran información útil en la consola de Postman:

- `Login successful. Token saved.`
- `User registered: ID=1`
- `Account created: ID=1`
- `Card created: ID=1`
- `Deposit created: Transaction ID=5`
- `New balance: 2000.00`

Para ver la consola: **View → Show Postman Console** (Alt+Ctrl+C)

---

## 🆘 Solución de Problemas

### Los servicios no responden
```bash
# Verifica que todos los servicios estén registrados en Eureka
curl http://localhost:8761/eureka/apps
```

### Token expirado
- El token tiene una duración de 24 horas
- Ejecuta "Login" nuevamente para obtener un nuevo token

### Variables no se actualizan
1. Abre la consola de Postman (Alt+Ctrl+C)
2. Verifica que los scripts se ejecuten sin errores
3. Revisa que la response sea exitosa (200, 201)

---

## 🎓 Recursos Adicionales

- **AGENTS.md**: Guía de desarrollo y convenciones
- **README.md**: Documentación del proyecto
- **Endpoints Documentation**: Ver descripción en cada request de Postman

---

¡Disfruta probando la API de Digital Money House! 🚀
