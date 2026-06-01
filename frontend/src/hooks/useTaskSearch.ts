import { useQuery } from '@tanstack/react-query';
import { taskApi } from '../api/client';
import { Task } from '../types';

export interface SearchFilters {
  status?: string;
  priority?: string;
  assigneeId?: number;
  projectId?: number;
  query?: string;
  dueBefore?: string;
  overdue?: boolean;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const useTaskSearch = (filters: SearchFilters) => {
  return useQuery<PageResponse<Task>>({
    queryKey: ['tasks-search', filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (filters.status) params.append('status', filters.status);
      if (filters.priority) params.append('priority', filters.priority);
      if (filters.assigneeId) params.append('assigneeId', String(filters.assigneeId));
      if (filters.projectId) params.append('projectId', String(filters.projectId));
      if (filters.query) params.append('query', filters.query);
      if (filters.dueBefore) params.append('dueBefore', filters.dueBefore);
      if (filters.overdue) params.append('overdue', 'true');
      params.append('page', String(filters.page || 0));
      params.append('size', String(filters.size || 20));

      const { data } = await taskApi.get(`/api/tasks/search?${params.toString()}`);
      return data;
    },
    enabled: Object.values(filters).some(v => v !== undefined && v !== '' && v !== null),
  });
};
