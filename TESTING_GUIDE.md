# Guía de Pruebas - Sistema de Autenticación y Autorización

## 📋 Resumen del Sistema

El sistema ahora está completamente configurado con:

- ✅ **Roles predefinidos**: `ROLE_USER` y `ROLE_ADMIN`
- ✅ **Usuario administrador por defecto**: `admin/admin123`
- ✅ **Configuración JWT con roles**
- ✅ **Endpoints protegidos por roles**
- ✅ **Inicialización automática de datos**

## 🧪 Casos de Prueba

### 1. Verificar Inicialización de Datos

Al iniciar el `fp_micro_authservice`, deberías ver en los logs:

```
Rol ROLE_USER creado exitosamente
Rol ROLE_ADMIN creado exitosamente
Usuario administrador creado exitosamente:
  Username: admin
  Password: admin123
  Roles: ROLE_ADMIN
```

### 2. Probar Registro de Usuario Nuevo

**Endpoint**: `POST http://localhost:8081/auth/api/register`

**Body**:

```json
{
  "username": "testuser",
  "password": "password123"
}
```

**Resultado esperado**:

- Respuesta: `"User registered successfully"`
- El usuario se crea automáticamente con `ROLE_USER`

### 3. Probar Autenticación OAuth2

**Paso 1**: Obtener código de autorización

```
GET http://localhost:8080/auth/oauth2/authorize?response_type=code&client_id=client&redirect_uri=http://localhost:3000/callback&scope=openid
```

**Paso 2**: Intercambiar código por token

```
POST http://localhost:8081/auth/oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic Y2xpZW50OnNlY3JldA==

grant_type=authorization_code&code={CODIGO}&redirect_uri=http://localhost:3000/callback
```

### 4. Probar Endpoints Protegidos del Gateway

#### A. Endpoint Público (Sin autenticación)

```
GET http://localhost:8080/gateway/health/public
```

**Resultado esperado**: ✅ Acceso permitido sin token

#### B. Endpoint para Usuarios (Requiere ROLE_USER o ROLE_ADMIN)

```
GET http://localhost:8080/gateway/health/user
Authorization: Bearer {JWT_TOKEN}
```

**Resultado esperado**:

- ✅ Con `testuser` (ROLE_USER): Acceso permitido
- ✅ Con `admin` (ROLE_ADMIN): Acceso permitido

#### C. Endpoint Solo Administradores (Requiere ROLE_ADMIN)

```
GET http://localhost:8080/gateway/health
Authorization: Bearer {JWT_TOKEN}
```

**Resultado esperado**:

- ❌ Con `testuser` (ROLE_USER): 403 Forbidden
- ✅ Con `admin` (ROLE_ADMIN): Acceso permitido

## 🔍 Verificar Contenido del JWT

Puedes decodificar el JWT en https://jwt.io para verificar que contiene:

```json
{
  "sub": "admin",
  "aud": ["client"],
  "roles": ["ROLE_ADMIN"],
  "iss": "http://localhost:8081",
  "exp": 1735588800,
  "iat": 1735585200
}
```

## 🚀 Pasos para Ejecutar las Pruebas

1. **Iniciar servicios** (en este orden):

   ```bash
   # Config Server
   cd fp_servers/fp_micro_configserver
   ./mvnw spring-boot:run

   # Discovery Server
   cd fp_servers/fp_micro_discoveryserver
   ./mvnw spring-boot:run

   # Auth Service
   cd fp_micro_authservice
   ./mvnw spring-boot:run

   # Gateway
   cd fp_servers/fp_micro_gateway
   ./mvnw spring-boot:run
   ```

2. **Verificar logs** de cada servicio para confirmar que inician correctamente

3. **Ejecutar pruebas** usando herramientas como:
   - Postman
   - curl
   - Insomnia
   - Extensión REST Client de VS Code

## 🛡️ Configuración de Seguridad Implementada

### En el Gateway (`SecurityConfig.java`)

- **Públicos**: `/auth/**`, `/actuator/**`, `/gateway/health/public`
- **Solo ADMIN**: `/gateway/health`
- **USER o ADMIN**: `/gateway/health/user`
- **Autenticados**: Cualquier otro endpoint

### En el Auth Service

- **Roles automáticos**: Nuevos usuarios → `ROLE_USER`
- **Usuario admin**: `admin/admin123` → `ROLE_ADMIN`
- **JWT personalizado**: Incluye claim `roles`

## 📝 Notas Importantes

1. **Orden de inicio**: Los servicios deben iniciarse en el orden especificado
2. **Base de datos**: Se usa H2 en memoria, los datos se recrean en cada reinicio
3. **Puertos**:
   - Config Server: 8888
   - Discovery Server: 8761
   - Auth Service: 8081
   - Gateway: 8080
4. **Frontend**: Configurado para http://localhost:3000

## 🔧 Troubleshooting

### Error: "Default role not found"

- **Causa**: El `DataInitializer` no se ejecutó
- **Solución**: Verificar que el componente esté registrado y que la base de datos esté disponible

### Error: 403 Forbidden en endpoints protegidos

- **Causa**: JWT no contiene los roles correctos
- **Solución**: Verificar el contenido del JWT y la configuración del `jwtAuthenticationConverter`

### Error: JWT inválido

- **Causa**: Configuración incorrecta del JWT decoder
- **Solución**: Verificar que el Gateway esté configurado para usar el Auth Service como servidor de autorización
