import { TaskStatus } from '../types';

// Mapa de transiciones válidas (replica la lógica del backend)
export const VALID_TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  TODO: ['IN_PROGRESS'],
  IN_PROGRESS: ['IN_REVIEW', 'TODO'],
  IN_REVIEW: ['DONE', 'IN_PROGRESS'],
  DONE: [], // Estado final
};

export const canTransition = (from: TaskStatus, to: TaskStatus): boolean => {
  return VALID_TRANSITIONS[from].includes(to);
};
