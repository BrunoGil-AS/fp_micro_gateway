# Gateway OAuth2 Configuration

## Configuración del Gateway para Autenticación con Auth Service

El gateway ha sido configurado para autenticar con el servicio de autenticación (auth service) usando OAuth2 y JWT tokens.

### Componentes Configurados

#### 1. Dependencias Maven

- `spring-boot-starter-oauth2-resource-server`: Para validar JWT tokens

#### 2. Configuración de Seguridad (SecurityConfig.java)

- Habilita OAuth2 Resource Server con validación JWT
- Configura rutas públicas: `/auth/**`, `/oauth2/**`, `/login`, `/actuator/**`, `/gateway/health`
- Requiere autenticación para todas las demás rutas

#### 3. Configuración de Propiedades (fp_micro_gateway.properties)

- `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`: URL para obtener las claves públicas JWT
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`: URL del emisor de tokens
- Rutas configuradas para redirigir a auth service

#### 4. Filtro JWT Personalizado (JwtAuthenticationFilter.java)

- Valida tokens JWT en las peticiones
- Extrae información del usuario y la agrega a los headers para servicios downstream
- Headers agregados: `X-User-Id`, `X-User-Roles`

#### 5. Configuración JWT (JwtConfig.java)

- Bean para decodificar tokens JWT usando las claves del auth service

### Flujo de Autenticación

1. **Sin token**: Las rutas públicas son accesibles sin autenticación
2. **Con token válido**:
   - El gateway valida el JWT contra el auth service
   - Extrae información del usuario y la agrega a los headers
   - Redirige la petición al servicio correspondiente
3. **Con token inválido**: Retorna 401 Unauthorized

### Endpoints de Prueba

- `GET /gateway/health`: Verificar que el gateway está funcionando
- `GET /gateway/user-info`: Mostrar información del usuario autenticado (requiere token)

### URLs de Auth Service Expuestas

- `/auth/**`: Endpoints de la API del auth service
- `/oauth2/**`: Endpoints OAuth2 (token, authorize, etc.)
- `/login`: Página de login

### Configuración de Clientes OAuth2

El auth service tiene configurados los siguientes clientes:

1. **Gateway Client** (`fp_micro_gateway`)

   - Client Credentials Grant
   - Scopes: `gateway.read`, `gateway.write`
   - Secret: `12345`

2. **Frontend Client** (`fp_frontend`)
   - Authorization Code Grant
   - Público (sin secreto)
   - Redirect URI: `http://localhost:3000/callback`
   - Scopes: `openid`, `profile`, `api.read`

### Cómo Usar

1. **Obtener token para el frontend**:

   ```
   POST http://localhost:8080/oauth2/token
   Authorization: Basic <client_credentials>
   Content-Type: application/x-www-form-urlencoded

   grant_type=authorization_code&
   code=<authorization_code>&
   redirect_uri=http://localhost:3000/callback
   ```

2. **Usar token en peticiones**:

   ```
   GET http://localhost:8080/some-protected-endpoint
   Authorization: Bearer <jwt_token>
   ```

3. **Verificar autenticación**:
   ```
   GET http://localhost:8080/gateway/user-info
   Authorization: Bearer <jwt_token>
   ```

### Puertos

- Gateway: `8080`
- Auth Service: `8081`
- Config Server: `8888`
- Discovery Server: `8761`
