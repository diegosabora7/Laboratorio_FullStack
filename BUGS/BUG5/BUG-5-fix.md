# Bug 5 — Auth: Token refresh causa loop infinito

## Síntoma
Cuando el access token expira, en lugar de refrescar silenciosamente, la app redirige al login. En la consola del navegador se ven múltiples requests 401 en loop: cada petición que recibe 401 intenta hacer refresh independientemente, generando un bucle infinito de requests.

## Ubicación
`frontend/src/api/client.ts`

## Causa raíz
El interceptor de respuesta de axios no tenía mecanismo para evitar reintentos múltiples. Cuando varias peticiones estaban en vuelo simultáneamente y todas recibían 401 (token expirado), cada una disparaba su propio refresh de forma independiente:

1. Petición A → 401 → intenta refresh → reintenta A
2. Petición B → 401 → intenta refresh → reintenta B
3. Petición C → 401 → intenta refresh → reintenta C
4. Si el refresh falla → cada una redirige a login (múltiples redirects)

### Código con el bug
```typescript
taskApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401) {
      // BUG: Cada petición que recibe 401 intenta refresh independientemente
      try {
        const { data } = await authApi.post('/api/auth/refresh', ...);
        originalRequest.headers.Authorization = `Bearer ${data.token}`;
        return taskApi(originalRequest); // Puede generar otro 401 → loop
      } catch {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

## Corrección aplicada

```typescript
taskApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Flag _retry: solo permite UN intento de refresh por petición
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const authStorage = JSON.parse(localStorage.getItem('auth-storage') || '{}');
        const token = authStorage?.state?.token;

        // Axios puro (no taskApi) para evitar que el interceptor capture esta petición
        const { data } = await axios.post('http://localhost:8082/api/auth/refresh', null, {
          headers: { Authorization: `Bearer ${token}` },
        });

        // Actualizar Zustand persist storage
        const newStorage = { ...authStorage, state: { ...authStorage.state, token: data.token } };
        localStorage.setItem('auth-storage', JSON.stringify(newStorage));

        // Reintentar la petición original con el nuevo token
        originalRequest.headers.Authorization = `Bearer ${data.token}`;
        return taskApi(originalRequest);
      } catch (refreshError) {
        // Refresh falló → logout definitivo
        localStorage.removeItem('auth-storage');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

## Conceptos clave

### Flag `_retry` (solución mínima)
Marca cada request con `_retry = true` después del primer intento de refresh. Si la petición reintentada vuelve a recibir 401, no se vuelve a intentar refresh → se propaga el error.

### Axios puro vs instancia con interceptor
Se usa `axios.post(...)` directamente (no `authApi` ni `taskApi`) para la petición de refresh. Esto evita que el interceptor de respuesta capture un posible 401 del refresh y genere recursión.

### Mejora adicional (no implementada): Cola de peticiones
Para un escenario con muchas peticiones simultáneas, la solución óptima es:
1. Un flag global `isRefreshing`
2. Una cola `failedQueue` donde se encolan las peticiones que reciben 401
3. Solo la primera petición hace refresh
4. Cuando el refresh termina, se resuelven todas las peticiones en cola con el nuevo token

## Verificación
1. Login → navegar a Dashboard (polling activo)
2. Cuando el token expire, el interceptor refresca silenciosamente
3. No hay loop de requests 401 en la consola
4. Si el refresh token también expiró → redirige a login una sola vez

## Impacto de no corregirlo
- Loop infinito de requests HTTP (puede saturar el navegador y el servidor)
- El usuario ve un flash de redirects antes de llegar al login
- Posible bloqueo de la cuenta si el servidor detecta demasiados intentos de refresh
- Mala experiencia de usuario: en vez de refresh silencioso, se pierde la sesión
