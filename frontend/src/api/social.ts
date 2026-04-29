import { api } from "./client";

export interface MatchSuggestionResponse {
  userId: number;
  userName: string;
  firstName: string;
  lastName: string;
  sharedCourses: string;
  compatibilityScore: number;
}

export interface InvitationResponse {
  invitationId: number;
  groupId: number;
  groupName: string;
  invitedByUserId: number;
  invitedByFirstName: string;
  invitedByLastName: string;
  invitedUserId: number;
  status: string;
  createdTimestamp: string;
  respondedTimestamp: string | null;
}

export interface ReviewEntry {
  reviewId: number;
  userId: number;
  userName: string;
  roomId: number;
  rating: number;
  comment: string | null;
  createdTimestamp: string;
}

const AVATAR_PALETTE = [
  "#e74c3c", "#e67e22", "#27ae60", "#8e44ad",
  "#2980b9", "#16a085", "#c0392b", "#d35400",
];

export function avatarColor(userId: number): string {
  return AVATAR_PALETTE[userId % AVATAR_PALETTE.length];
}

export const socialApi = {
  getSuggestions: (userId: number, limit = 10) =>
    api.get<MatchSuggestionResponse[]>(`/matches/suggestions?userId=${userId}&limit=${limit}`),

  searchMatches: (userId: number, course?: string, minScore?: number) => {
    const params = new URLSearchParams({ userId: String(userId) });
    if (course) params.set("course", course);
    if (minScore !== undefined) params.set("minScore", String(minScore));
    return api.get<MatchSuggestionResponse[]>(`/matches/search?${params}`);
  },

  getPendingInvites: (userId: number) =>
    api.get<InvitationResponse[]>(`/study-groups/invites?userId=${userId}`),

  createGroup: (ownerUserId: number, groupName: string, primaryCourse?: string) =>
    api.post<{ groupId: number }>("/study-groups", {
      ownerUserId,
      groupName,
      description: "",
      visibility: "INVITE_ONLY",
      primaryCourse: primaryCourse ?? "",
    }),

  inviteToGroup: (groupId: number, invitedByUserId: number, invitedUserId: number) =>
    api.post<InvitationResponse>(`/study-groups/${groupId}/invites`, {
      invitedByUserId,
      invitedUserId,
    }),

  acceptInvite: (inviteId: number, userId: number) =>
    api.post<InvitationResponse>(`/study-groups/invites/${inviteId}/accept`, { userId }),

  declineInvite: (inviteId: number, userId: number) =>
    api.post<InvitationResponse>(`/study-groups/invites/${inviteId}/decline`, { userId }),

  getReviews: (roomId: string) =>
    api.get<ReviewEntry[]>(`/api/reviews?roomId=${roomId}`),
};
