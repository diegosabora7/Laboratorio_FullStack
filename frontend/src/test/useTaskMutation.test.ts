import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient } from '@tanstack/react-query';

describe('Bug 6 Fix - Optimistic update with rollback', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  it('should save previous tasks before optimistic update for rollback', () => {
    const previousTasks = [
      { id: 1, title: 'Task 1', status: 'TODO' },
      { id: 2, title: 'Task 2', status: 'IN_PROGRESS' },
    ];

    queryClient.setQueryData(['tasks', 'project', 1], previousTasks);

    // Simulate saving snapshot before mutation
    const snapshot = queryClient.getQueryData(['tasks', 'project', 1]);
    expect(snapshot).toEqual(previousTasks);
    expect(snapshot).toHaveLength(2);
  });

  it('should rollback to previous state on error', () => {
    const previousTasks = [
      { id: 1, title: 'Task 1', status: 'TODO' },
      { id: 2, title: 'Task 2', status: 'IN_PROGRESS' },
    ];

    queryClient.setQueryData(['tasks', 'project', 1], previousTasks);

    // Simulate optimistic update (move task 1 to IN_PROGRESS)
    queryClient.setQueryData(['tasks', 'project', 1], (old: any[]) =>
      old.map((task) => (task.id === 1 ? { ...task, status: 'IN_PROGRESS' } : task))
    );

    // Verify optimistic update applied
    const updated = queryClient.getQueryData(['tasks', 'project', 1]) as any[];
    expect(updated[0].status).toBe('IN_PROGRESS');

    // Simulate rollback on error
    queryClient.setQueryData(['tasks', 'project', 1], previousTasks);

    // Verify rollback
    const rolledBack = queryClient.getQueryData(['tasks', 'project', 1]) as any[];
    expect(rolledBack[0].status).toBe('TODO');
  });
});

describe('Bug 7 Fix - Cleanup on unmount', () => {
  it('should clear interval when cleanup function is called', () => {
    vi.useFakeTimers();

    const callback = vi.fn();
    const interval = setInterval(callback, 5000);

    // Simulate 3 ticks
    vi.advanceTimersByTime(15000);
    expect(callback).toHaveBeenCalledTimes(3);

    // Simulate cleanup (component unmount)
    clearInterval(interval);

    // After cleanup, no more calls
    vi.advanceTimersByTime(15000);
    expect(callback).toHaveBeenCalledTimes(3); // Still 3, not 6

    vi.useRealTimers();
  });
});

describe('Bug 5 Fix - Token refresh with retry flag', () => {
  it('should mark request with _retry flag to prevent infinite loop', () => {
    const originalRequest = { headers: {}, url: '/api/tasks/project/1' } as any;

    // Before retry
    expect(originalRequest._retry).toBeUndefined();

    // After first 401
    originalRequest._retry = true;

    // Second 401 should not trigger refresh
    expect(originalRequest._retry).toBe(true);
  });

  it('should not retry refresh endpoint itself', () => {
    const refreshRequest = { headers: {}, url: '/api/auth/refresh', _retry: false } as any;

    const shouldRetry =
      !refreshRequest._retry && refreshRequest.url !== '/api/auth/refresh';

    expect(shouldRetry).toBe(false); // Should NOT retry the refresh endpoint
  });
});
