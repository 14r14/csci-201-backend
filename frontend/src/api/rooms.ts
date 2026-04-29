import { api } from "./client";
import type { Room, RoomFeature } from "../types/room";

export interface RoomApiResponse {
  roomId: number;
  buildingName: string;
  roomNumber: string;
  capacity: number;
  featureList: string[];
  mapLocation: string | null;
  currentStatus: "AVAILABLE" | "OCCUPIED" | "MAINTENANCE" | "RESERVED";
  averageRating: number;
  ratingsCount: number;
  waitlistCount: number;
}

const KNOWN_FEATURES = new Set<string>([
  "whiteboard", "monitors", "outlets", "projector", "window", "printer",
]);

function adaptStatus(s: RoomApiResponse["currentStatus"]): Room["currentStatus"] {
  if (s === "AVAILABLE") return "available";
  return "booked";
}

function bookingIntensity(s: RoomApiResponse["currentStatus"]): number {
  if (s === "AVAILABLE") return 0.2;
  if (s === "MAINTENANCE") return 0.5;
  return 0.9;
}

export function adaptRoom(r: RoomApiResponse): Room {
  return {
    roomID: String(r.roomId),
    buildingName: r.buildingName,
    roomNumber: r.roomNumber,
    capacity: r.capacity,
    featureList: r.featureList.filter((f): f is RoomFeature => KNOWN_FEATURES.has(f)),
    mapLocation: { x: 0, y: 0 },
    currentStatus: adaptStatus(r.currentStatus),
    averageRating: r.averageRating ?? 0,
    ratingNoise: r.averageRating ?? 0,
    ratingCleanliness: r.averageRating ?? 0,
    reviewCount: r.ratingsCount ?? 0,
    waitlistCount: r.waitlistCount ?? 0,
    bookingIntensity: bookingIntensity(r.currentStatus),
  };
}

export const roomsApi = {
  getAll: () => api.get<RoomApiResponse[]>("/api/rooms"),
  getById: (id: number) => api.get<RoomApiResponse>(`/api/rooms/${id}`),
};
