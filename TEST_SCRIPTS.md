# Test Scripts para Gateway OAuth2

## 1. Verificar que el gateway está funcionando

```bash
curl -X GET http://localhost:8080/gateway/health
```

## 2. Registrar un nuevo usuario

```bash
curl -X POST http://localhost:8080/auth/api/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass"
  }'
```

## 3. Obtener token de autorización (necesita cliente configurado)

### Para Client Credentials (gateway)

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Basic $(echo -n 'fp_micro_gateway:12345' | base64)" \
  -d "grant_type=client_credentials&scope=gateway.read gateway.write"
```

### Para Authorization Code (frontend) - Paso 1: Obtener código

```
# Abrir en navegador:
http://localhost:8080/oauth2/authorize?response_type=code&client_id=fp_frontend&redirect_uri=http://localhost:3000/callback&scope=openid profile api.read
```

### Para Authorization Code (frontend) - Paso 2: Intercambiar código por token

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code&client_id=fp_frontend&redirect_uri=http://localhost:3000/callback&code=YOUR_CODE_HERE"
```

## 4. Probar endpoint protegido

```bash
curl -X GET http://localhost:8080/gateway/user-info \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## 5. Probar acceso a otros servicios a través del gateway

```bash
# Ejemplo: acceder a un servicio protegido
curl -X GET http://localhost:8080/some-service/protected-endpoint \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## 6. Verificar endpoints públicos

```bash
# Login page
curl -X GET http://localhost:8080/login

# OAuth2 endpoints
curl -X GET http://localhost:8080/oauth2/jwks
```

## Comandos de Base64 para Autenticación Básica

### Para Windows PowerShell:

```powershell
$credentials = "fp_micro_gateway:12345"
$encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($credentials))
Write-Output $encoded
```

### Para Linux/Mac:

```bash
echo -n 'fp_micro_gateway:12345' | base64
```

## Notas Importantes

1. **Asegúrate de que los servicios estén ejecutándose en el orden correcto:**

   - Config Server (puerto 8888)
   - Discovery Server (puerto 8761)
   - Auth Service (puerto 8081)
   - Gateway (puerto 8080)

2. **Los tokens JWT tienen expiración**, así que deberás obtener nuevos tokens periódicamente.

3. **Para development**, puedes usar herramientas como Postman o Insomnia para facilitar las pruebas.
