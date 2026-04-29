import { useEffect, useState } from "react";
import { socialApi, avatarColor, type InvitationResponse, type MatchSuggestionResponse } from "../api/social";
import { useAuth } from "../context/AuthContext";
import { timeAgo } from "../utils/timeAgo";
import "./SocialPage.css";

// ─── Types ────────────────────────────────────────────────────────────────────

interface StudyPartner {
  id: number;
  name: string;
  initials: string;
  sharedCourses: string[];
  compatibilityScore: number;
  status: "online" | "offline" | "studying";
  avatarColor: string;
}

interface StudyRequest {
  id: number;
  fromName: string;
  fromInitials: string;
  sharedCourses: string[];
  sentAt: string;
  avatarColor: string;
}

// ─── Adapters ─────────────────────────────────────────────────────────────────

function adaptPartner(m: MatchSuggestionResponse): StudyPartner {
  const courses = m.sharedCourses
    ? m.sharedCourses.split(",").map(s => s.trim()).filter(Boolean)
    : [];
  return {
    id: m.userId,
    name: `${m.firstName} ${m.lastName}`,
    initials: `${m.firstName[0] ?? ""}${m.lastName[0] ?? ""}`.toUpperCase(),
    sharedCourses: courses,
    compatibilityScore: Math.min(100, Math.round(m.compatibilityScore * 20)),
    status: "offline",
    avatarColor: avatarColor(m.userId),
  };
}

function adaptInvitation(inv: InvitationResponse): StudyRequest {
  const firstName = inv.invitedByFirstName ?? "";
  const lastName = inv.invitedByLastName ?? "";
  const name = `${firstName} ${lastName}`.trim() || `User #${inv.invitedByUserId}`;
  const initials = `${firstName[0] ?? ""}${lastName[0] ?? ""}`.toUpperCase() || "?";
  return {
    id: inv.invitationId,
    fromName: name,
    fromInitials: initials,
    sharedCourses: inv.groupName ? [inv.groupName] : [],
    sentAt: timeAgo(inv.createdTimestamp),
    avatarColor: avatarColor(inv.invitedByUserId),
  };
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function StatusDot({ status }: { status: StudyPartner["status"] }) {
  return <span className={`status-dot status-${status}`} title={status} />;
}

function ScoreBar({ score }: { score: number }) {
  const color = score >= 90 ? "#e74c3c" : score >= 75 ? "#e67e22" : "#f1c40f";
  return (
    <div className="score-bar-track">
      <div className="score-bar-fill" style={{ width: `${score}%`, background: color }} />
    </div>
  );
}

function PartnerCard({
  partner,
  onSendRequest,
  requestSent,
}: {
  partner: StudyPartner;
  onSendRequest: (id: number) => void;
  requestSent: boolean;
}) {
  return (
    <div className="partner-card">
      <div className="partner-card-top">
        <div className="avatar" style={{ background: partner.avatarColor }}>
          {partner.initials}
          <StatusDot status={partner.status} />
        </div>
        <div className="partner-info">
          <div className="partner-name">{partner.name}</div>
          <div className="partner-status-label">{partner.status}</div>
        </div>
        <div className="compatibility-badge">
          <span className="compat-number">{partner.compatibilityScore}</span>
          <span className="compat-label">match</span>
        </div>
      </div>

      <ScoreBar score={partner.compatibilityScore} />

      <div className="shared-courses-label">Shared courses</div>
      <div className="course-tags">
        {partner.sharedCourses.map((c) => (
          <span key={c} className="course-tag">{c}</span>
        ))}
      </div>

      <button
        className={`send-request-btn ${requestSent ? "sent" : ""}`}
        onClick={() => onSendRequest(partner.id)}
        disabled={requestSent}
      >
        {requestSent ? "✓ Request Sent" : "Send Study Request"}
      </button>
    </div>
  );
}

function IncomingRequestCard({
  request,
  onAccept,
  onDecline,
  resolved,
  resolution,
}: {
  request: StudyRequest;
  onAccept: (id: number) => void;
  onDecline: (id: number) => void;
  resolved: boolean;
  resolution: "accepted" | "declined" | null;
}) {
  return (
    <div className={`request-card ${resolved ? `resolved-${resolution}` : ""}`}>
      <div className="avatar avatar-sm" style={{ background: request.avatarColor }}>
        {request.fromInitials}
      </div>
      <div className="request-info">
        <div className="request-name">{request.fromName}</div>
        <div className="course-tags">
          {request.sharedCourses.map((c) => (
            <span key={c} className="course-tag course-tag-sm">{c}</span>
          ))}
        </div>
        <div className="request-time">{request.sentAt}</div>
      </div>
      {resolved ? (
        <div className={`resolution-label ${resolution}`}>
          {resolution === "accepted" ? "✓ Accepted" : "✗ Declined"}
        </div>
      ) : (
        <div className="request-actions">
          <button className="btn-accept" onClick={() => onAccept(request.id)}>Accept</button>
          <button className="btn-decline" onClick={() => onDecline(request.id)}>Decline</button>
        </div>
      )}
    </div>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

export default function SocialPage() {
  const { user } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const [partners, setPartners] = useState<StudyPartner[]>([]);
  const [requests, setRequests] = useState<StudyRequest[]>([]);
  const [sentRequests, setSentRequests] = useState<Set<number>>(new Set());
  const [resolvedRequests, setResolvedRequests] = useState<Map<number, "accepted" | "declined">>(new Map());
  const [loadingPartners, setLoadingPartners] = useState(true);
  const [loadingRequests, setLoadingRequests] = useState(true);

  useEffect(() => {
    if (!user?.userId) {
      setLoadingPartners(false);
      setLoadingRequests(false);
      return;
    }
    socialApi.getSuggestions(user.userId)
      .then(data => setPartners(data.map(adaptPartner)))
      .catch(() => setPartners([]))
      .finally(() => setLoadingPartners(false));

    socialApi.getPendingInvites(user.userId)
      .then(data => setRequests(data.map(adaptInvitation)))
      .catch(() => setRequests([]))
      .finally(() => setLoadingRequests(false));
  }, [user?.userId]);

  const filteredPartners = partners.filter(
    (p) =>
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.sharedCourses.some((c) => c.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const handleSendRequest = async (partnerId: number) => {
    if (!user?.userId) return;
    const partner = partners.find(p => p.id === partnerId);
    try {
      const group = await socialApi.createGroup(
        user.userId,
        `Study session with ${partner?.name ?? "you"}`,
        partner?.sharedCourses[0],
      );
      await socialApi.inviteToGroup(group.groupId, user.userId, partnerId);
      setSentRequests((prev) => new Set(prev).add(partnerId));
    } catch {
      setSentRequests((prev) => new Set(prev).add(partnerId));
    }
  };

  const handleAccept = async (invitationId: number) => {
    if (!user?.userId) return;
    try {
      await socialApi.acceptInvite(invitationId, user.userId);
    } catch { /* already accepted or error; optimistic update below */ }
    setResolvedRequests((prev) => new Map(prev).set(invitationId, "accepted"));
  };

  const handleDecline = async (invitationId: number) => {
    if (!user?.userId) return;
    try {
      await socialApi.declineInvite(invitationId, user.userId);
    } catch { /* already declined or error; optimistic update below */ }
    setResolvedRequests((prev) => new Map(prev).set(invitationId, "declined"));
  };

  const pendingCount = requests.filter(r => !resolvedRequests.has(r.id)).length;

  return (
    <div className="social-page">
      <div className="social-header">
        <div className="social-header-text">
          <h1 className="social-title">Study Partners</h1>
          <p className="social-subtitle">Find classmates who share your courses</p>
        </div>
        <div className="header-stats">
          <div className="stat-chip">
            <span className="stat-num">{loadingPartners ? "…" : partners.length}</span> matches
          </div>
          <div className="stat-chip stat-chip-alert">
            <span className="stat-num">{loadingRequests ? "…" : pendingCount}</span> requests
          </div>
        </div>
      </div>

      {!user?.userId && (
        <div style={{ padding: "1rem 1.5rem", background: "var(--surface)", borderRadius: "var(--radius)", margin: "1rem", color: "var(--text-muted)" }}>
          Sign in to see your study partner matches.
        </div>
      )}

      <div className="search-wrapper">
        <span className="search-icon">⌕</span>
        <input
          className="search-bar"
          type="text"
          placeholder="Search by name or course (e.g. CSCI 201)"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        {searchQuery && (
          <button className="search-clear" onClick={() => setSearchQuery("")}>×</button>
        )}
      </div>

      <div className="social-layout">
        <section className="partners-section">
          <h2 className="section-title">
            Recommended Partners
            {searchQuery && (
              <span className="filter-note">
                {" "}— {filteredPartners.length} result{filteredPartners.length !== 1 ? "s" : ""}
              </span>
            )}
          </h2>

          {loadingPartners ? (
            <div className="empty-state"><div>Loading matches…</div></div>
          ) : filteredPartners.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">🔍</div>
              <div>{searchQuery ? `No partners found for "${searchQuery}"` : "No matches found. Try adding courses to your profile."}</div>
            </div>
          ) : (
            <div className="partners-grid">
              {filteredPartners.map((partner) => (
                <PartnerCard
                  key={partner.id}
                  partner={partner}
                  onSendRequest={handleSendRequest}
                  requestSent={sentRequests.has(partner.id)}
                />
              ))}
            </div>
          )}
        </section>

        <section className="requests-section">
          <h2 className="section-title">
            Incoming Requests
            {pendingCount > 0 && <span className="badge-dot" />}
          </h2>

          {loadingRequests ? (
            <div className="empty-state"><div>Loading requests…</div></div>
          ) : requests.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">📭</div>
              <div>No pending requests</div>
            </div>
          ) : (
            <div className="requests-list">
              {requests.map((req) => (
                <IncomingRequestCard
                  key={req.id}
                  request={req}
                  onAccept={handleAccept}
                  onDecline={handleDecline}
                  resolved={resolvedRequests.has(req.id)}
                  resolution={resolvedRequests.get(req.id) ?? null}
                />
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
