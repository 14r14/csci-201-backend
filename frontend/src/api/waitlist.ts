import { api } from "./client";

export interface WaitlistResponse {
  waitlistId: number;
  userId: number;
  roomId: number;
  requestedTimeSlot: string;
  queuePosition: number;
  waitlistCount: number;
  message: string;
}

export const waitlistApi = {
  join: (userId: number, roomId: number, requestedTimeSlot: string) =>
    api.post<WaitlistResponse>("/api/waitlist", { userId, roomId, requestedTimeSlot }),

  leave: (waitlistId: number) =>
    api.del<void>(`/api/waitlist/${waitlistId}`),

  getByUser: (userId: number) =>
    api.get<WaitlistResponse[]>(`/api/waitlist?userId=${userId}`),
};
