# Bug 7 — Memory Leak: Polling sin cleanup

## Síntoma
Al navegar entre páginas, la consola mostraba warnings de "Can't perform a React state update on an unmounted component". El componente `ProjectDashboard` seguía haciendo requests HTTP cada 5 segundos al endpoint `/api/tasks/project/1/stats` incluso después de que el usuario salía de la página Dashboard.

## Ubicación
`frontend/src/components/Dashboard/ProjectDashboard.tsx`

## Causa raíz
El `useEffect` creaba un `setInterval` para hacer polling cada 5 segundos, pero **no retornaba una función de cleanup** que ejecutara `clearInterval`. Cuando React desmontaba el componente (al navegar a otra ruta), el intervalo seguía activo en memoria, disparando `setStats()` sobre un componente que ya no existía en el DOM.

### Código con el bug
```tsx
useEffect(() => {
  const fetchStats = async () => { /* ... */ };
  fetchStats();
  const interval = setInterval(() => { fetchStats(); }, 5000);
  // Sin return — no hay cleanup
}, []);
```

## Corrección aplicada
Se agregó la función de cleanup en el `return` del `useEffect` que ejecuta `clearInterval(interval)` al desmontar el componente:

```tsx
useEffect(() => {
  const fetchStats = async () => { /* ... */ };
  fetchStats();
  const interval = setInterval(() => { fetchStats(); }, 5000);

  return () => {
    console.log('Componente desmontado....');
    clearInterval(interval);
  };
}, []);
```

## Concepto clave
En React, todo `useEffect` que crea suscripciones, timers, o listeners debe retornar una función de limpieza (cleanup function). React la ejecuta automáticamente cuando:
- El componente se desmonta
- Las dependencias del effect cambian (antes de re-ejecutar el effect)

## Verificación
1. Navegar a Dashboard → se ven logs `[ProjectDashboard] 🔄 Fetching stats...` cada 5s
2. Navegar a Kanban → se ve log `Componente desmontado....` y los logs de polling **se detienen**
3. No hay más warnings de state updates en componentes desmontados

## Impacto de no corregirlo
- Fuga de memoria progresiva (cada visita al Dashboard crea un nuevo intervalo que nunca se limpia)
- Requests HTTP innecesarios al backend
- Posibles inconsistencias de estado si el usuario vuelve al Dashboard (múltiples intervalos corriendo en paralelo)
- Degradación de performance del navegador con el tiempo
