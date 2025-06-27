# Configuración OAuth2 - Gateway y Auth Service

## Resumen

Este documento explica cómo funciona la integración OAuth2 entre el API Gateway y el servicio de autenticación.

## Arquitectura

```
Cliente -> Gateway (Puerto 8080) -> Auth Service (Puerto 8081)
```

## Flujo de Autenticación

### 1. Obtener Token de Acceso

El cliente debe autenticarse con el servicio de autenticación para obtener un JWT:

```bash
# Ejemplo con client credentials (para el gateway)
curl -X POST http://localhost:8081/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "fp_micro_gateway:12345" \
  -d "grant_type=client_credentials&scope=gateway.read gateway.write"
```

### 2. Usar el Token

Una vez obtenido el token, incluirlo en las peticiones al gateway:

```bash
curl -H "Authorization: Bearer <JWT_TOKEN>" \
  http://localhost:8080/gateway/test
```

### 3. Renovar Token (Refresh Token)

Para aplicaciones frontend, usar refresh tokens para obtener nuevos access tokens sin reautenticación:

```bash
# Renovar token usando refresh token
curl -X POST http://localhost:8081/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=<REFRESH_TOKEN>&client_id=fp_frontend"
```

## Endpoints Importantes

### Auth Service (Puerto 8081)

- `/oauth2/token` - Endpoint para obtener tokens
- `/oauth2/jwks` - Conjunto de claves públicas para validar JWTs
- `/auth/api/register` - Registro de usuarios

### Gateway (Puerto 8080)

- `/gateway/test` - Endpoint de prueba (requiere autenticación)
- `/gateway/health` - Health check (no requiere autenticación)
- `/auth/**` - Proxy hacia el auth service (sin autenticación)

## Configuración

### Gateway (`fp_micro_gateway.properties`)

- Configurado como OAuth2 Resource Server
- Valida JWTs usando el endpoint JWKs del auth service
- Aplica filtros de autenticación a rutas protegidas

### Auth Service (`fp_micro_authservice.properties`)

- Configurado como OAuth2 Authorization Server
- Genera y firma JWTs
- Expone endpoint JWKs para validación

## Clientes OAuth2 Configurados

1. **fp_micro_gateway**: Cliente para el gateway (client_credentials)
2. **fp_frontend**: Cliente para aplicaciones frontend (authorization_code + refresh_token)
   - Access Token: válido por 15 minutos
   - Refresh Token: válido por 30 días
   - Reutilización de refresh tokens: deshabilitada (se genera nuevo refresh token)

## Implementación en el Frontend

### Estructura de Tokens

Cuando el frontend se autentica, recibe:

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 900,
  "scope": "openid profile api.read api.write"
}
```

### Gestión Automática de Tokens

El frontend debe implementar:

1. **Almacenamiento Seguro**:

   ```javascript
   // Almacenar tokens (considera usar httpOnly cookies para mayor seguridad)
   localStorage.setItem("access_token", response.access_token);
   localStorage.setItem("refresh_token", response.refresh_token);
   localStorage.setItem(
     "token_expires_at",
     Date.now() + response.expires_in * 1000
   );
   ```

2. **Interceptor de Peticiones**:

   ```javascript
   // Axios interceptor para agregar token automáticamente
   axios.interceptors.request.use(async (config) => {
     const token = await getValidToken(); // Función que verifica y renueva si es necesario
     if (token) {
       config.headers.Authorization = `Bearer ${token}`;
     }
     return config;
   });
   ```

3. **Función de Renovación Automática**:

   ```javascript
   async function getValidToken() {
     const accessToken = localStorage.getItem("access_token");
     const expiresAt = localStorage.getItem("token_expires_at");

     // Verificar si el token está por expirar (con 2 minutos de margen)
     if (Date.now() >= expiresAt - 120000) {
       return await refreshToken();
     }

     return accessToken;
   }

   async function refreshToken() {
     const refreshToken = localStorage.getItem("refresh_token");

     try {
       const response = await fetch("http://localhost:8081/oauth2/token", {
         method: "POST",
         headers: {
           "Content-Type": "application/x-www-form-urlencoded",
         },
         body: new URLSearchParams({
           grant_type: "refresh_token",
           refresh_token: refreshToken,
           client_id: "fp_frontend",
         }),
       });

       if (response.ok) {
         const data = await response.json();

         // Actualizar tokens almacenados
         localStorage.setItem("access_token", data.access_token);
         localStorage.setItem("refresh_token", data.refresh_token);
         localStorage.setItem(
           "token_expires_at",
           Date.now() + data.expires_in * 1000
         );

         return data.access_token;
       } else {
         // Refresh token inválido, redirigir a login
         logout();
         return null;
       }
     } catch (error) {
       console.error("Error renovando token:", error);
       logout();
       return null;
     }
   }

   function logout() {
     localStorage.removeItem("access_token");
     localStorage.removeItem("refresh_token");
     localStorage.removeItem("token_expires_at");
     window.location.href = "/login";
   }
   ```

4. **Manejo de Respuestas 401**:
   ```javascript
   // Interceptor para manejar tokens expirados
   axios.interceptors.response.use(
     (response) => response,
     async (error) => {
       if (error.response?.status === 401) {
         const newToken = await refreshToken();
         if (newToken) {
           // Reintentar la petición original con el nuevo token
           error.config.headers.Authorization = `Bearer ${newToken}`;
           return axios.request(error.config);
         }
       }
       return Promise.reject(error);
     }
   );
   ```

### Flujo Recomendado para el Frontend

1. **Autenticación Inicial**: Usar Authorization Code Flow con PKCE
2. **Almacenamiento**: Guardar access_token y refresh_token
3. **Uso Automático**: Interceptor agrega token a todas las peticiones
4. **Renovación Proactiva**: Renovar token 2 minutos antes de que expire
5. **Manejo de Errores**: Si refresh falla, redirigir a login

### Consideraciones de Seguridad

- **Almacenamiento**: Considera usar httpOnly cookies en lugar de localStorage
- **HTTPS**: Siempre usar HTTPS en producción
- **Rotación**: Los refresh tokens se rotan automáticamente para mayor seguridad
- **Expiración**: Los tokens tienen tiempos de vida limitados

## Pruebas

1. **Verificar Auth Service**:

   ```bash
   curl http://localhost:8081/oauth2/jwks
   ```

2. **Verificar Gateway sin autenticación**:

   ```bash
   curl http://localhost:8080/gateway/health
   ```

3. **Verificar Gateway con autenticación**:

   ```bash
   # Primero obtener token
   TOKEN=$(curl -s -X POST http://localhost:8081/oauth2/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -u "fp_micro_gateway:12345" \
     -d "grant_type=client_credentials&scope=gateway.read" \
     | jq -r '.access_token')

   # Usar token
   curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/gateway/test
   ```

4. **Probar renovación de tokens (frontend)**:

   ```bash
   # Simular flujo de autorización para obtener refresh token
   # (En producción esto se haría a través del navegador)

   # 1. Obtener código de autorización (normalmente a través del navegador)
   # GET http://localhost:8081/oauth2/authorize?response_type=code&client_id=fp_frontend&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20api.read

   # 2. Intercambiar código por tokens (simulado)
   TOKENS=$(curl -s -X POST http://localhost:8081/oauth2/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=authorization_code&code=<AUTHORIZATION_CODE>&redirect_uri=http://localhost:3000/callback&client_id=fp_frontend")

   # 3. Extraer refresh token
   REFRESH_TOKEN=$(echo $TOKENS | jq -r '.refresh_token')

   # 4. Usar refresh token para obtener nuevo access token
   NEW_TOKENS=$(curl -s -X POST http://localhost:8081/oauth2/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=refresh_token&refresh_token=$REFRESH_TOKEN&client_id=fp_frontend")

   echo "Nuevos tokens: $NEW_TOKENS"
   ```

## Troubleshooting

- **401 Unauthorized**: Verificar que el token sea válido y no haya expirado
- **JWT decode error**: Verificar que el auth service esté ejecutándose y el endpoint JWKs sea accesible
- **Connection refused**: Verificar que ambos servicios estén ejecutándose en los puertos correctos
- **Refresh token invalid**: El refresh token ha expirado (30 días) o ha sido revocado - reautenticación necesaria
- **Token rotation issues**: Verificar que el frontend use el nuevo refresh token devuelto en cada renovación
- **CORS errors**: Asegurar que el auth service permita requests desde el dominio del frontend

## Resumen de Cambios para Refresh Tokens

### Backend (Auth Service)

1. **ClientConfig.java**: Habilitado `REFRESH_TOKEN` grant type para cliente frontend
2. **TokenSettings**: Configurados tiempos de vida (15 min access, 30 días refresh)
3. **Token rotation**: Deshabilitada reutilización para mayor seguridad

### Frontend (Implementación Requerida)

1. **Almacenamiento**: Guardar access_token, refresh_token y tiempo de expiración
2. **Interceptors**: Automáticamente agregar tokens y manejar renovación
3. **Renovación proactiva**: Renovar tokens antes de que expiren
4. **Manejo de errores**: Logout automático si refresh falla

### Beneficios

- **UX mejorada**: No se requiere reautenticación frecuente
- **Seguridad**: Tokens de corta duración con renovación segura
- **Automatización**: Gestión transparente de tokens para el usuario
