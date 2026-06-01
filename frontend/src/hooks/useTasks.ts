import { useQuery } from '@tanstack/react-query';
import { taskApi } from '../api/client';
import { Task } from '../types';

export const useTasksByProject = (projectId: number) => {
  return useQuery<Task[]>({
    queryKey: ['tasks', 'project', projectId],
    queryFn: async () => {
      const { data } = await taskApi.get(`/api/tasks/project/${projectId}`);
      return data;
    },
    enabled: !!projectId,
  });
};
