# Bug 6 — UX: Tarea desaparece al fallar drag & drop

## Síntoma
Al mover una tarea en el Kanban, si la API falla (por conflicto de concurrencia, error de red, etc.), la tarea queda en un estado inconsistente en la UI — muestra un status que no se guardó en el servidor. Hay que refrescar la página para ver el estado real.

## Ubicación
`frontend/src/hooks/useTaskMutation.ts`

## Causa raíz
El optimistic update movía la tarea inmediatamente en la caché de React Query, pero:
1. No se guardaba un snapshot del estado previo
2. En `onError` no se hacía rollback al estado anterior
3. En `onSettled` no se invalidaba la query para resincronizar con el servidor

### Código con el bug
```typescript
onMutate: async ({ taskId, status }) => {
  await queryClient.cancelQueries({ queryKey: ['tasks', 'project', projectId] });
  queryClient.setQueryData(['tasks', 'project', projectId], (old) => {
    return old.map((task) => task.id === taskId ? { ...task, status } : task);
  });
  // No retorna contexto para rollback
},
onError: (err) => {
  console.error('Error:', err);
  // No hay rollback
},
onSettled: () => {
  // No invalida queries
},
```

## Corrección aplicada

```typescript
onMutate: async ({ taskId, status }) => {
  await queryClient.cancelQueries({ queryKey: ['tasks', 'project', projectId] });

  // 1. Guardar snapshot ANTES del optimistic update
  const previousTasks = queryClient.getQueryData(['tasks', 'project', projectId]);

  // 2. Optimistic update
  queryClient.setQueryData(['tasks', 'project', projectId], (old) => {
    return old.map((task) => task.id === taskId ? { ...task, status } : task);
  });

  // 3. Retornar contexto para rollback
  return { previousTasks };
},
onError: (err, _variables, _context) => {
  console.error('Error updating task status:', err);
  // 4. Rollback: restaurar estado previo
  if (_context?.previousTasks) {
    queryClient.setQueryData(['tasks', 'project', projectId], _context.previousTasks);
  }
},
onSettled: () => {
  // 5. Siempre resincronizar con el servidor
  queryClient.invalidateQueries({ queryKey: ['tasks', 'project', projectId] });
},
```

## Conceptos clave

### Optimistic Update con rollback (patrón de React Query)
1. **onMutate** → Guardar estado previo + aplicar cambio optimista + retornar contexto
2. **onError** → Usar el contexto para revertir al estado previo
3. **onSettled** → Invalidar queries para garantizar consistencia con el servidor (se ejecuta siempre, éxito o fallo)

### ¿Por qué `invalidateQueries` en `onSettled`?
Incluso si la mutación fue exitosa, el servidor puede haber aplicado transformaciones adicionales (timestamps, validaciones). Invalidar la query fuerza un refetch silencioso que garantiza que la UI refleje exactamente lo que tiene el servidor.

## Verificación
1. Mover una tarea en el Kanban con task-service arriba → se mueve y se guarda correctamente
2. Provocar un conflicto (modificar la tarea desde Postman, luego moverla en el Kanban) → la tarea vuelve a su posición original automáticamente
3. No es necesario refrescar la página manualmente

## Impacto de no corregirlo
- La UI muestra información falsa (un status que no se guardó)
- El usuario cree que su acción fue exitosa cuando no lo fue
- Confusión en equipos colaborativos donde el tablero muestra estados diferentes para cada usuario
- Pérdida de confianza en la herramienta
