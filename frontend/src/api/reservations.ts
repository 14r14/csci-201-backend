import { api } from "./client";

export interface ReservationResponse {
  reservationId: number;
  userId: number;
  roomId: number;
  buildingName: string;
  roomNumber: string;
  startTime: string;
  endTime: string;
  status: string;
  createdTimestamp: string;
}

export const reservationsApi = {
  book: (userId: number, roomId: number, startTime: string, endTime: string) =>
    api.post<ReservationResponse>("/api/reservations", { userId, roomId, startTime, endTime }),

  cancel: (reservationId: number) =>
    api.del<ReservationResponse>(`/api/reservations/${reservationId}`),

  getByUser: (userId: number) =>
    api.get<ReservationResponse[]>(`/api/reservations?userId=${userId}`),
};

/** Parse "9:00–10:00 AM" style labels into today's ISO-8601 UTC strings. */
export function parseSlotLabel(label: string): { startTime: string; endTime: string } {
  const cleaned = label.replace("–", "-").replace(/\s*(AM|PM)\s*/gi, " $1");
  const parts = cleaned.split("-").map(s => s.trim());
  const suffix = /PM/i.test(label) ? "PM" : "AM";
  const startRaw = parts[0].includes("AM") || parts[0].includes("PM") ? parts[0] : `${parts[0]} ${suffix}`;
  const endRaw = parts[1] ?? "";

  function toDate(timeStr: string): Date {
    const d = new Date();
    d.setSeconds(0, 0);
    const match = timeStr.match(/(\d+):(\d+)\s*(AM|PM)/i);
    if (!match) return d;
    let h = parseInt(match[1], 10);
    const m = parseInt(match[2], 10);
    const meridiem = match[3].toUpperCase();
    if (meridiem === "PM" && h !== 12) h += 12;
    if (meridiem === "AM" && h === 12) h = 0;
    d.setHours(h, m);
    return d;
  }

  return {
    startTime: toDate(startRaw).toISOString(),
    endTime: toDate(endRaw).toISOString(),
  };
}
