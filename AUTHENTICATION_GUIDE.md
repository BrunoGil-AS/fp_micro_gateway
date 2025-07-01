# Guía de Autenticación y Autorización - Gateway Security

## Resumen de Cambios Realizados

### 1. Configuración del Gateway (`SecurityConfig.java`)

Se ha configurado el gateway para:

- **Extraer roles del JWT**: El convertidor `jwtAuthenticationConverter()` extrae los roles del token JWT
- **Restringir acceso por roles**: La línea `.pathMatchers("/gateway/health").hasRole("ADMIN")` ahora funciona correctamente
- **Múltiples fuentes de roles**: Busca roles en los claims `authorities`, `roles`, o `scope`

### 2. Servicio de Autenticación (`DataInitializer.java`)

Se agregó inicialización automática de datos:

- **Roles por defecto**: Crea `ROLE_USER` y `ROLE_ADMIN` automáticamente
- **Usuario administrador**: Crea un usuario `admin` con password `admin123` y rol `ROLE_ADMIN`

## Cómo Funciona el Sistema

### Flujo de Autenticación:

1. **Login**: Usuario se autentica en `/auth/**` (permitido públicamente)
2. **Token JWT**: El servicio de auth emite un JWT con roles en el claim "roles"
3. **Verificación**: El gateway verifica el JWT y extrae los roles
4. **Autorización**: Se verifica si el usuario tiene el rol requerido

### Endpoints y Permisos:

```java
// Acceso público
.pathMatchers("/auth/**").permitAll()
.pathMatchers("/actuator/**").permitAll()

// Solo usuarios con ROLE_ADMIN
.pathMatchers("/gateway/health").hasRole("ADMIN")

// Cualquier usuario autenticado
.anyExchange().authenticated()
```

## Usuarios y Roles Disponibles

### Usuario Administrador (creado automáticamente):

- **Username**: `admin`
- **Password**: `admin123`
- **Roles**: `ROLE_ADMIN`

### Usuarios Regulares:

- Se registran con `ROLE_USER` por defecto
- Pueden registrarse vía `/auth/api/register`

## Testing del Sistema

### 1. Verificar que el servicio de auth esté funcionando:

```bash
# Verificar health check público
curl http://localhost:8080/actuator/health

# Registrar un nuevo usuario
curl -X POST http://localhost:8080/auth/api/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

### 2. Obtener token JWT:

```bash
# Flujo OAuth2 Authorization Code
# 1. Ir a: http://localhost:8080/oauth2/authorize?response_type=code&client_id=your-client-id&redirect_uri=your-redirect-uri
# 2. Login con admin/admin123
# 3. Intercambiar código por token
```

### 3. Probar endpoint restringido:

```bash
# Acceso con token válido de ADMIN
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8080/gateway/health

# Debería retornar 200 OK si el token contiene ROLE_ADMIN
# Debería retornar 403 Forbidden si el token solo tiene ROLE_USER
```

## Estructura del JWT

El token JWT incluye los roles en el claim "roles":

```json
{
  "sub": "admin",
  "roles": ["ROLE_ADMIN"],
  "iat": 1640995200,
  "exp": 1640998800
}
```

## Solución de Problemas

### Error: "Default role not found"

- **Causa**: No existen roles en la base de datos
- **Solución**: Reiniciar el servicio de auth para que `DataInitializer` cree los roles

### Error: 403 Forbidden en `/gateway/health`

- **Causa**: El token no contiene `ROLE_ADMIN`
- **Solución**: Usar el usuario `admin` o asignar rol ADMIN al usuario

### Error: JWT validation failed

- **Causa**: Token inválido o expirado
- **Solución**: Obtener un nuevo token del servicio de auth

## Próximos Pasos

1. **Configurar cliente OAuth2**: En `ClientConfig.java` del servicio de auth
2. **Integrar con frontend**: Configurar flujo OAuth2 en React
3. **Persistencia**: Cambiar de H2 a base de datos productiva
4. **Monitoreo**: Agregar logs de seguridad
