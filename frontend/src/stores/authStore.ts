import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  userId: number | null;
  name: string | null;
  email: string | null;
  isAuthenticated: boolean;
  login: (data: { token: string; userId: number; name: string; email: string }) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userId: null,
      name: null,
      email: null,
      isAuthenticated: false,
      login: (data) => set({ ...data, isAuthenticated: true }),
      logout: () =>
        set({
          token: null,
          userId: null,
          name: null,
          email: null,
          isAuthenticated: false,
        }),
    }),
    { name: 'auth-storage' }
  )
);
