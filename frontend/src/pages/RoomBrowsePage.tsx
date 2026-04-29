import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BuildingAvailabilityMap } from "../components/BuildingAvailabilityMap";
import type { Room, RoomFeature } from "../types/room";
import { roomsApi, adaptRoom } from "../api/rooms";
import { reservationsApi, parseSlotLabel } from "../api/reservations";
import { waitlistApi } from "../api/waitlist";
import { socialApi, type ReviewEntry } from "../api/social";
import { useAuth } from "../context/AuthContext";

const FEATURE_LABELS: Record<RoomFeature, string> = {
  whiteboard: "Whiteboard",
  monitors: "Monitors",
  outlets: "Power outlets",
  projector: "Projector",
  window: "Natural light",
  printer: "Printer",
};

const ALL_FEATURES = Object.keys(FEATURE_LABELS) as RoomFeature[];

function sortRooms(rooms: Room[], mode: "building" | "rating"): Room[] {
  const copy = [...rooms];
  if (mode === "rating") {
    copy.sort((a, b) => b.averageRating - a.averageRating);
    return copy;
  }
  copy.sort((a, b) => {
    const bld = a.buildingName.localeCompare(b.buildingName);
    if (bld !== 0) return bld;
    return a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true });
  });
  return copy;
}

function statusLabel(s: Room["currentStatus"]): string {
  if (s === "available") return "Open";
  if (s === "booked") return "Booked";
  return "Partial";
}

type TimeSlot = { id: string; label: string };

function mockTimeSlotsForRoom(room: Room): TimeSlot[] {
  const n = 3 + (room.roomID.charCodeAt(room.roomID.length - 1) % 3);
  const labels = [
    "9:00–10:00 AM",
    "10:30–11:30 AM",
    "12:00–1:00 PM",
    "2:00–3:00 PM",
    "3:30–4:30 PM",
    "5:00–6:00 PM",
    "7:00–8:00 PM",
  ];
  return labels.slice(0, n).map((label, i) => ({ id: `${room.roomID}-slot-${i}`, label }));
}

function ScorePicker({
  id,
  label,
  value,
  onChange,
}: {
  id: string;
  label: string;
  value: number;
  onChange: (n: number) => void;
}) {
  return (
    <div className="score-picker">
      <span className="score-picker__label" id={`${id}-label`}>
        {label}
      </span>
      <div className="score-picker__row" role="group" aria-labelledby={`${id}-label`}>
        {([1, 2, 3, 4, 5] as const).map((n) => (
          <button
            key={n}
            type="button"
            className={`score-picker__btn${value === n ? " score-picker__btn--active" : ""}`}
            aria-pressed={value === n}
            onClick={() => onChange(n)}
          >
            {n}
          </button>
        ))}
      </div>
    </div>
  );
}

export function RoomBrowsePage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAuthenticated = !!user && !user.guest;

  const [rooms, setRooms] = useState<Room[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [roomsError, setRoomsError] = useState<string | null>(null);

  const [sortMode, setSortMode] = useState<"building" | "rating">("building");
  const [featureFilters, setFeatureFilters] = useState<Set<RoomFeature>>(new Set());
  const [availableOnly, setAvailableOnly] = useState(false);
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [reviewsOpenId, setReviewsOpenId] = useState<string | null>(null);
  const [reviewsCache, setReviewsCache] = useState<Record<string, ReviewEntry[]>>({});
  const [jumpRoomId, setJumpRoomId] = useState<string>("");
  const [bookingPopoverRoomId, setBookingPopoverRoomId] = useState<string | null>(null);
  const [bookingSlotChoiceId, setBookingSlotChoiceId] = useState<string | null>(null);
  const [bookingDemoNotice, setBookingDemoNotice] = useState<string | null>(null);
  const [bookingLoading, setBookingLoading] = useState(false);
  const bookPopoverRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const [ratingPopoverRoomId, setRatingPopoverRoomId] = useState<string | null>(null);
  const [rateOverall, setRateOverall] = useState(0);
  const [rateNoise, setRateNoise] = useState(0);
  const [rateCleanliness, setRateCleanliness] = useState(0);
  const [rateReview, setRateReview] = useState("");
  const [rateDemoNotice, setRateDemoNotice] = useState<string | null>(null);
  const [rateLoading, setRateLoading] = useState(false);
  const ratePopoverRefs = useRef<Record<string, HTMLDivElement | null>>({});

  useEffect(() => {
    roomsApi.getAll()
      .then(rs => setRooms(rs.map(adaptRoom)))
      .catch(err => setRoomsError(err instanceof Error ? err.message : "Failed to load rooms"))
      .finally(() => setLoadingRooms(false));
  }, []);

  const filtered = useMemo(() => {
    let list = rooms.filter((r) => {
      for (const f of featureFilters) {
        if (!r.featureList.includes(f)) return false;
      }
      if (availableOnly && r.currentStatus !== "available") return false;
      return true;
    });
    list = sortRooms(list, sortMode);
    return list;
  }, [rooms, sortMode, featureFilters, availableOnly]);

  const selectedRoom = useMemo(
    () => filtered.find((r) => r.roomID === selectedRoomId) ?? null,
    [filtered, selectedRoomId],
  );

  const selectedBuildingName = selectedRoom?.buildingName ?? null;

  function resetRateForm() {
    setRateOverall(0);
    setRateNoise(0);
    setRateCleanliness(0);
    setRateReview("");
    setRateDemoNotice(null);
  }

  function closeBookingPopover() {
    setBookingPopoverRoomId(null);
    setBookingSlotChoiceId(null);
    setBookingDemoNotice(null);
  }

  function closeRatingPopover() {
    setRatingPopoverRoomId(null);
    resetRateForm();
  }

  useEffect(() => {
    if (bookingPopoverRoomId === null && ratingPopoverRoomId === null) return;
    const close = (e: MouseEvent) => {
      const t = e.target as Node;
      if (bookingPopoverRoomId && bookPopoverRefs.current[bookingPopoverRoomId]?.contains(t)) return;
      if (ratingPopoverRoomId && ratePopoverRefs.current[ratingPopoverRoomId]?.contains(t)) return;
      closeBookingPopover();
      closeRatingPopover();
    };
    const timerId = window.setTimeout(() => document.addEventListener("click", close), 0);
    return () => {
      window.clearTimeout(timerId);
      document.removeEventListener("click", close);
    };
  }, [bookingPopoverRoomId, ratingPopoverRoomId]);

  useEffect(() => {
    if (bookingPopoverRoomId === null && ratingPopoverRoomId === null) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        closeBookingPopover();
        closeRatingPopover();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [bookingPopoverRoomId, ratingPopoverRoomId]);

  function toggleFeature(f: RoomFeature) {
    setFeatureFilters((prev) => {
      const next = new Set(prev);
      if (next.has(f)) next.delete(f);
      else next.add(f);
      return next;
    });
  }

  function onJumpSelect(value: string) {
    setJumpRoomId(value);
    if (!value) return;
    setSelectedRoomId(value);
    document.getElementById(`room-row-${value}`)?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  function onSelectBuilding(buildingName: string) {
    const first = filtered.find((r) => r.buildingName === buildingName);
    if (!first) return;
    setSelectedRoomId(first.roomID);
    setJumpRoomId(first.roomID);
    document.getElementById(`room-row-${first.roomID}`)?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }

  async function loadReviews(roomId: string) {
    if (reviewsCache[roomId]) return;
    const data = await socialApi.getReviews(roomId).catch(() => []);
    setReviewsCache(prev => ({ ...prev, [roomId]: data }));
  }

  async function handleBookConfirm(room: Room) {
    if (!user?.userId || !bookingSlotChoiceId) return;
    const slotLabel = mockTimeSlotsForRoom(room).find(s => s.id === bookingSlotChoiceId)?.label ?? "";
    setBookingLoading(true);
    try {
      const { startTime, endTime } = parseSlotLabel(slotLabel);
      await reservationsApi.book(user.userId, Number(room.roomID), startTime, endTime);
      setBookingDemoNotice(`Booked ${slotLabel} in ${room.buildingName} ${room.roomNumber}.`);
      // Refresh room list to reflect new status
      roomsApi.getAll().then(rs => setRooms(rs.map(adaptRoom))).catch(() => null);
    } catch (err) {
      setBookingDemoNotice(err instanceof Error ? err.message : "Booking failed.");
    } finally {
      setBookingLoading(false);
    }
  }

  async function handleWaitlistJoin(room: Room) {
    if (!user?.userId) return;
    const slot = new Date().toISOString();
    try {
      const res = await waitlistApi.join(user.userId, Number(room.roomID), slot);
      alert(`Added to waitlist. Position: ${res.queuePosition} of ${res.waitlistCount}`);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Could not join waitlist.");
    }
  }

  async function handleRateSubmit(room: Room) {
    if (!user?.userId || rateOverall < 1) return;
    setRateLoading(true);
    try {
      const comment = `noise:${rateNoise},cleanliness:${rateCleanliness}|${rateReview.trim()}`;
      await socialApi.getReviews(room.roomID); // warm cache before overwriting
      await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/reviews`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId: user.userId, roomId: Number(room.roomID), rating: rateOverall, comment }),
      });
      setRateDemoNotice(`Rated ${rateOverall}/5 — thank you!`);
      // Refresh room to show updated averageRating
      roomsApi.getById(Number(room.roomID))
        .then(updated => setRooms(prev => prev.map(r => r.roomID === room.roomID ? adaptRoom(updated) : r)))
        .catch(() => null);
      // Bust review cache for this room
      setReviewsCache(prev => { const next = { ...prev }; delete next[room.roomID]; return next; });
    } catch (err) {
      setRateDemoNotice(err instanceof Error ? err.message : "Rating failed.");
    } finally {
      setRateLoading(false);
    }
  }

  if (loadingRooms) {
    return <div style={{ padding: "2rem", textAlign: "center" }}>Loading rooms…</div>;
  }

  if (roomsError) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <p>Failed to load rooms: {roomsError}</p>
        <button onClick={() => { setRoomsError(null); setLoadingRooms(true); roomsApi.getAll().then(rs => setRooms(rs.map(adaptRoom))).catch(e => setRoomsError(e.message)).finally(() => setLoadingRooms(false)); }}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="browse-page">
      <header className="browse-header">
        <div className="browse-header__brand">
          <span className="browse-header__mark" aria-hidden="true" />
          <div>
            <h1 className="browse-header__title">Study room availability</h1>
            <p className="browse-header__subtitle">
              Real-time view. {isAuthenticated ? `Signed in as ${user.userName}.` : "Browsing as guest — sign in to book."}
            </p>
          </div>
        </div>
        {!isAuthenticated && (
          <button className="btn-link" onClick={() => navigate("/login")} style={{ marginLeft: "auto" }}>
            Log in to book
          </button>
        )}
      </header>

      <div className="browse-toolbar">
        <div className="toolbar-row">
          <label className="field">
            <span className="field__label">Quick room</span>
            <select
              className="field__control"
              value={jumpRoomId}
              onChange={(e) => onJumpSelect(e.target.value)}
              aria-label="Jump to a study room in the list"
            >
              <option value="">Select a room…</option>
              {sortRooms(rooms, "building").map((r) => (
                <option key={r.roomID} value={r.roomID}>
                  {r.buildingName} · {r.roomNumber}
                  {r.currentStatus === "available" ? " — open" : ""}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span className="field__label">Sort by</span>
            <select
              className="field__control"
              value={sortMode}
              onChange={(e) => setSortMode(e.target.value as "building" | "rating")}
            >
              <option value="building">Building &amp; room number</option>
              <option value="rating">Room rating</option>
            </select>
          </label>

          <label className="toggle">
            <input
              type="checkbox"
              checked={availableOnly}
              onChange={(e) => setAvailableOnly(e.target.checked)}
            />
            <span>Only show open rooms</span>
          </label>
        </div>

        <fieldset className="feature-filters">
          <legend>Filter by resources</legend>
          <div className="feature-filters__chips">
            {ALL_FEATURES.map((f) => (
              <label key={f} className="chip">
                <input
                  type="checkbox"
                  checked={featureFilters.has(f)}
                  onChange={() => toggleFeature(f)}
                />
                <span>{FEATURE_LABELS[f]}</span>
              </label>
            ))}
          </div>
        </fieldset>
      </div>

      <div className="browse-grid">
        <section className="panel panel--map" aria-labelledby="map-heading">
          <h2 id="map-heading" className="panel__title">Buildings</h2>
          <BuildingAvailabilityMap
            rooms={filtered}
            selectedBuildingName={selectedBuildingName}
            onSelectBuilding={onSelectBuilding}
          />
        </section>

        <section className="panel panel--list" aria-labelledby="list-heading">
          <div className="panel__head">
            <h2 id="list-heading" className="panel__title">Rooms ({filtered.length})</h2>
          </div>

          <ul className="room-list">
            {filtered.map((room) => {
              const bookDisabled = !isAuthenticated || room.currentStatus !== "available";
              const waitlistDisabled = !isAuthenticated || room.currentStatus === "available";
              const rateDisabled = !isAuthenticated;

              const bookLabel = !isAuthenticated
                ? "Book room. Sign in required."
                : room.currentStatus !== "available"
                  ? "Book room. This room is not available."
                  : "Book this room.";

              const waitlistLabel = !isAuthenticated
                ? "Join waitlist. Sign in required."
                : room.currentStatus === "available"
                  ? "Join waitlist. Only available when room is fully booked."
                  : "Join the waitlist for this room.";

              const rateLabel = !isAuthenticated
                ? "Rate room. Sign in required."
                : "Rate this study room.";

              return (
                <li key={room.roomID} id={`room-row-${room.roomID}`} className="room-card">
                  <div className="room-card__top">
                    <div>
                      <h3 className="room-card__title">
                        {room.buildingName}{" "}
                        <span className="room-card__number">{room.roomNumber}</span>
                      </h3>
                      <p className="room-card__meta">
                        Capacity {room.capacity} ·{" "}
                        {room.featureList.map((f) => FEATURE_LABELS[f]).join(" · ")}
                      </p>
                    </div>
                    <span className={`badge badge--${room.currentStatus}`} title="Availability">
                      {statusLabel(room.currentStatus)}
                    </span>
                  </div>

                  <div className="room-card__ratings">
                    <div className="stars" aria-label={`Average rating ${room.averageRating} out of 5`}>
                      <span className="stars__value">{room.averageRating.toFixed(1)}</span>
                      <span className="stars__out">/5</span>
                      <span className="stars__count">({room.reviewCount} reviews)</span>
                    </div>
                    <div className="sub-ratings">
                      <div>
                        <span className="sub-ratings__label">Noise</span>
                        <meter min={0} max={5} low={2.5} high={3.5} optimum={5} value={room.ratingNoise}>
                          {room.ratingNoise}
                        </meter>
                        <span className="sub-ratings__num">{room.ratingNoise.toFixed(1)}</span>
                      </div>
                      <div>
                        <span className="sub-ratings__label">Clean</span>
                        <meter min={0} max={5} low={2.5} high={3.5} optimum={5} value={room.ratingCleanliness}>
                          {room.ratingCleanliness}
                        </meter>
                        <span className="sub-ratings__num">{room.ratingCleanliness.toFixed(1)}</span>
                      </div>
                    </div>
                  </div>

                  {room.waitlistCount > 0 && (
                    <p className="waitlist-banner">
                      Waitlist: <strong>{room.waitlistCount}</strong> student
                      {room.waitlistCount === 1 ? "" : "s"} ahead
                    </p>
                  )}

                  <div className="room-card__actions">
                    <button
                      type="button"
                      className="btn btn--ghost"
                      onClick={() => {
                        const next = reviewsOpenId === room.roomID ? null : room.roomID;
                        setReviewsOpenId(next);
                        if (next) loadReviews(room.roomID);
                      }}
                      aria-expanded={reviewsOpenId === room.roomID}
                    >
                      {reviewsOpenId === room.roomID ? "Hide reviews" : "View reviews"}
                    </button>

                    {/* Book popover */}
                    <div
                      className="book-popover-root"
                      ref={(el) => { bookPopoverRefs.current[room.roomID] = el; }}
                    >
                      <button
                        type="button"
                        className="btn btn--primary"
                        disabled={bookDisabled}
                        aria-label={bookLabel}
                        aria-haspopup="dialog"
                        aria-expanded={bookingPopoverRoomId === room.roomID}
                        aria-controls={`booking-popover-${room.roomID}`}
                        title={!isAuthenticated ? "Sign in to reserve" : room.currentStatus !== "available" ? "Room not available" : "Pick a time slot"}
                        onClick={(e) => {
                          e.stopPropagation();
                          if (bookDisabled) return;
                          if (bookingPopoverRoomId === room.roomID) { closeBookingPopover(); return; }
                          closeRatingPopover();
                          setBookingDemoNotice(null);
                          setBookingSlotChoiceId(null);
                          setBookingPopoverRoomId(room.roomID);
                        }}
                      >
                        Book room
                      </button>
                      {bookingPopoverRoomId === room.roomID && (
                        <div
                          className="booking-popover"
                          id={`booking-popover-${room.roomID}`}
                          role="dialog"
                          aria-labelledby={`booking-popover-title-${room.roomID}`}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <h4 className="booking-popover__title" id={`booking-popover-title-${room.roomID}`}>
                            Available times · today
                          </h4>
                          <p className="booking-popover__room">
                            {room.buildingName} {room.roomNumber} · seats {room.capacity}
                          </p>
                          <ul className="booking-popover__features" aria-label="Room resources">
                            {room.featureList.map((f) => <li key={f}>{FEATURE_LABELS[f]}</li>)}
                          </ul>
                          <p className="booking-popover__sub">Choose one slot (1 hour blocks).</p>
                          <ul className="booking-popover__slots" role="listbox" aria-label="Open time slots">
                            {mockTimeSlotsForRoom(room).map((slot) => {
                              const picked = bookingSlotChoiceId === slot.id;
                              return (
                                <li key={slot.id}>
                                  <button
                                    type="button"
                                    className={`booking-slot${picked ? " booking-slot--picked" : ""}`}
                                    role="option"
                                    aria-selected={picked}
                                    onClick={() => setBookingSlotChoiceId(slot.id)}
                                  >
                                    {slot.label}
                                  </button>
                                </li>
                              );
                            })}
                          </ul>
                          <p className="booking-popover__policy">
                            You can hold at most one reservation per slot.
                          </p>
                          {bookingDemoNotice && (
                            <p className="booking-popover__notice" role="status">{bookingDemoNotice}</p>
                          )}
                          <div className="booking-popover__footer">
                            <button type="button" className="btn btn--ghost btn--small" onClick={() => closeBookingPopover()}>
                              Close
                            </button>
                            <button
                              type="button"
                              className="btn btn--primary btn--small"
                              disabled={!bookingSlotChoiceId || bookingLoading}
                              onClick={() => handleBookConfirm(room)}
                            >
                              {bookingLoading ? "Booking…" : "Confirm"}
                            </button>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Rate popover */}
                    <div
                      className="book-popover-root"
                      ref={(el) => { ratePopoverRefs.current[room.roomID] = el; }}
                    >
                      <button
                        type="button"
                        className="btn btn--ghost"
                        disabled={rateDisabled}
                        aria-label={rateLabel}
                        aria-haspopup="dialog"
                        aria-expanded={ratingPopoverRoomId === room.roomID}
                        aria-controls={`rating-popover-${room.roomID}`}
                        title={!isAuthenticated ? "Sign in to rate" : "Rate this study room"}
                        onClick={(e) => {
                          e.stopPropagation();
                          if (rateDisabled) return;
                          if (ratingPopoverRoomId === room.roomID) { closeRatingPopover(); return; }
                          closeBookingPopover();
                          resetRateForm();
                          setRatingPopoverRoomId(room.roomID);
                        }}
                      >
                        Rate
                      </button>
                      {ratingPopoverRoomId === room.roomID && (
                        <div
                          className="booking-popover"
                          id={`rating-popover-${room.roomID}`}
                          role="dialog"
                          aria-labelledby={`rating-popover-title-${room.roomID}`}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <h4 className="booking-popover__title" id={`rating-popover-title-${room.roomID}`}>
                            Rate this room
                          </h4>
                          <p className="booking-popover__room">{room.buildingName} {room.roomNumber}</p>
                          <p className="booking-popover__sub">1 = poor, 5 = excellent.</p>
                          <ScorePicker id={`${room.roomID}-overall`} label="Overall" value={rateOverall} onChange={setRateOverall} />
                          <ScorePicker id={`${room.roomID}-noise`} label="Noise level (quietness)" value={rateNoise} onChange={setRateNoise} />
                          <ScorePicker id={`${room.roomID}-clean`} label="Cleanliness" value={rateCleanliness} onChange={setRateCleanliness} />
                          <label className="booking-popover__review-label" htmlFor={`rate-review-${room.roomID}`}>
                            Review (optional)
                          </label>
                          <textarea
                            id={`rate-review-${room.roomID}`}
                            className="booking-popover__textarea"
                            rows={3}
                            maxLength={500}
                            value={rateReview}
                            onChange={(e) => setRateReview(e.target.value)}
                            placeholder="Write your review for other students"
                          />
                          {rateDemoNotice && (
                            <p className="booking-popover__notice" role="status">{rateDemoNotice}</p>
                          )}
                          <div className="booking-popover__footer">
                            <button type="button" className="btn btn--ghost btn--small" onClick={() => closeRatingPopover()}>
                              Close
                            </button>
                            <button
                              type="button"
                              className="btn btn--primary btn--small"
                              disabled={rateOverall < 1 || rateNoise < 1 || rateCleanliness < 1 || rateLoading}
                              onClick={() => handleRateSubmit(room)}
                            >
                              {rateLoading ? "Saving…" : "Submit"}
                            </button>
                          </div>
                        </div>
                      )}
                    </div>

                    <button
                      type="button"
                      className="btn btn--secondary"
                      disabled={waitlistDisabled}
                      aria-label={waitlistLabel}
                      title={
                        !isAuthenticated ? "Sign in to join a waitlist"
                          : room.currentStatus === "available" ? "Waitlist opens when room is booked"
                          : "Join the waitlist"
                      }
                      onClick={() => handleWaitlistJoin(room)}
                    >
                      Join waitlist
                    </button>
                  </div>

                  {reviewsOpenId === room.roomID && (
                    <div className="reviews-drawer">
                      <h4 className="reviews-drawer__title">Recent feedback</h4>
                      {(reviewsCache[room.roomID] ?? []).length === 0 ? (
                        <p style={{ color: "var(--text-muted)", fontSize: "0.875rem" }}>No reviews yet.</p>
                      ) : (
                        <ul>
                          {(reviewsCache[room.roomID] ?? []).map((rev) => (
                            <li key={rev.reviewId} className="review-line">
                              <strong>{rev.userName}</strong> — {rev.rating}/5
                              {rev.comment && (() => {
                                const userText = rev.comment.includes("|") ? rev.comment.split("|").slice(1).join("|") : rev.comment;
                                return userText ? ` — ${userText}` : null;
                              })()}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </li>
              );
            })}
          </ul>

          {filtered.length === 0 && (
            <p className="empty-state">No rooms match these filters. Try removing a resource filter.</p>
          )}
        </section>
      </div>

      {selectedRoom && (
        <aside className="detail-strip" aria-live="polite">
          <strong>Selected:</strong> {selectedRoom.buildingName} {selectedRoom.roomNumber} ·{" "}
          {statusLabel(selectedRoom.currentStatus)}
        </aside>
      )}
    </div>
  );
}
