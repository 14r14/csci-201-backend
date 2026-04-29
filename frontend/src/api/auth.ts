import { api } from "./client";

export interface SessionUser {
  userId: number | null;
  userName: string;
  firstName: string;
  lastName: string;
  role: string;
  guest: boolean;
}

interface AuthResponse {
  message: string;
  user: SessionUser;
}

export const authApi = {
  login: (userName: string, password: string) =>
    api.post<AuthResponse>("/auth/login", { userName, password }),

  signup: (userName: string, password: string, firstName: string, lastName: string) =>
    api.post<AuthResponse>("/auth/signup", { userName, password, firstName, lastName }),

  guest: () => api.post<AuthResponse>("/auth/guest", {}),
};
