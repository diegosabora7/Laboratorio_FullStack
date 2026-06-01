import { useQuery } from '@tanstack/react-query';
import { taskApi } from '../api/client';
import { Task } from '../types';

export interface ProjectStats {
  projectId: number;
  totalTasks: number;
  todoCount: number;
  inProgressCount: number;
  inReviewCount: number;
  doneCount: number;
  completionPercentage: number;
}

export const useProjectStats = (projectId: number) => {
  return useQuery<ProjectStats>({
    queryKey: ['project-stats', projectId],
    queryFn: async () => {
      const { data } = await taskApi.get(`/api/tasks/project/${projectId}/stats`);
      return data;
    },
    enabled: !!projectId,
    refetchInterval: 30000, // Refrescar cada 30s
  });
};

export const useOverdueTasks = (projectId: number) => {
  return useQuery<Task[]>({
    queryKey: ['overdue-tasks', projectId],
    queryFn: async () => {
      const { data } = await taskApi.get(`/api/tasks/project/${projectId}/overdue`);
      return data;
    },
    enabled: !!projectId,
  });
};
