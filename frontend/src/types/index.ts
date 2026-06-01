export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Task {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  projectId: number;
  projectName: string;
  assigneeId: number;
  assigneeName: string;
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Project {
  id: number;
  name: string;
  description: string;
  ownerId: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: string;
  activo: boolean;
  createdAt: string;
  updatedAt: string;
}
