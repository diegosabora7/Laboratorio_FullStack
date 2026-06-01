import { useMutation, useQueryClient } from '@tanstack/react-query';
import { taskApi } from '../api/client';
import { Task, TaskStatus } from '../types';

export const useUpdateTaskStatus = (projectId: number) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      taskId,
      status,
    }: {
      taskId: number;
      status: TaskStatus;
    }) => {
      const { data } = await taskApi.patch(
        `/api/tasks/${taskId}/status?status=${status}`
      );
      return data;
    },
    // TODO: [LABORATORIO - BUG 6] - Tarea desaparece al fallar drag & drop: el optimistic update mueve la tarea en la UI,
    // pero si la API falla, NO se revierte el cambio. La tarea queda en un estado inconsistente hasta refrescar la página.
    onMutate: async ({ taskId, status }) => {
      await queryClient.cancelQueries({ queryKey: ['tasks', 'project', projectId] });

      //Capturamos el estado de las tareas antes de hacer el Drag and Drop
      const previousTasks = queryClient.getQueryData(['tasks', 'project', projectId])

      // Optimistic update: mover la tarea al nuevo status inmediatamente en la UI
      queryClient.setQueryData(
        ['tasks', 'project', projectId],
        (old: Task[] | undefined) => {
          if (!old) return old;
          return old.map((task) =>
            task.id === taskId ? { ...task, status } : task
          );
        }
      );

      // TODO: [LABORATORIO - BUG 6] - No se guarda snapshot previo para rollback. Debería retornar { previousTasks } como contexto.
      //Retornamos previousTasks
      return {previousTasks};
    },
    onError: (err, _variables, _context) => {
      // TODO: [LABORATORIO - BUG 6] - Sin rollback: aquí se debería restaurar el estado previo con context.previousTasks, pero no existe.
      console.error('Error updating task status:', err);
      if(_context?.previousTasks){
        //Restauramos la cache a como estaba antes del D&D
        queryClient.setQueryData(['tasks', 'project', projectId], _context.previousTasks);
      }
    },
    
    //Siempre Exito o falla: Sincronizamos con la verdadera informacion
    onSettled: () => {
      //Invalidamos cache para obligar a React Query a hacer un GET silencioso
      // TODO: [LABORATORIO - BUG 6] - No se invalida la query tras el settle, dejando la UI desincronizada permanentemente si hubo error.
      queryClient.invalidateQueries({queryKey:['tasks', 'project', projectId]});
    },
  });
};
