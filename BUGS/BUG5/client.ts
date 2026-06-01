import axios from 'axios';

export const authApi = axios.create({ baseURL: 'http://localhost:8082' });
export const taskApi = axios.create({ baseURL: 'http://localhost:8081' });

// Interceptor para agregar token
taskApi.interceptors.request.use((config) => {
  const authStorage = JSON.parse(localStorage.getItem('auth-storage') || '{}');
  const token = authStorage?.state?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// TODO: [LABORATORIO - BUG 5] - Token refresh causa loop infinito: cuando múltiples peticiones reciben 401 simultáneamente,
// cada una intenta hacer refresh de forma independiente. No hay flag isRefreshing ni cola de peticiones pendientes.
// Esto genera un bucle de requests 401 → refresh → 401 → refresh hasta que redirige al login.
// 1. EL PARCHE: Modificamos el interceptor de respuesta
taskApi.interceptors.response.use(
  (response) => response, // Si todo sale bien, dejamos pasar la respuesta
  async (error) => {
    const originalRequest = error.config;

    // Condición de oro: Es error 401 AND no ha sido reintentado AND NO es la ruta de refresh
    if (
      error.response?.status === 401 &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;

      try {
        const authStorage = JSON.parse(localStorage.getItem('auth-storage') || '{}');
        const token = authStorage?.state?.token;

        // Usar axios puro para evitar que el interceptor lo capture
        const { data } = await axios.post('http://localhost:8082/api/auth/refresh', null, {
          headers: { Authorization: `Bearer ${token}` },
        });

        // Actualizar token en Zustand persist storage
        const newStorage = {
          ...authStorage,
          state: { ...authStorage.state, token: data.token },
        };
        localStorage.setItem('auth-storage', JSON.stringify(newStorage));

        originalRequest.headers.Authorization = `Bearer ${data.token}`;
        return taskApi(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('auth-storage');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
  }
);