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
2. **fp_frontend**: Cliente para aplicaciones frontend (authorization_code)

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

## Troubleshooting

- **401 Unauthorized**: Verificar que el token sea válido y no haya expirado
- **JWT decode error**: Verificar que el auth service esté ejecutándose y el endpoint JWKs sea accesible
- **Connection refused**: Verificar que ambos servicios estén ejecutándose en los puertos correctos
