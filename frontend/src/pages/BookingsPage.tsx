import { useEffect, useState } from "react";
import { reservationsApi, type ReservationResponse } from "../api/reservations";
import { useAuth } from "../context/AuthContext";
import "./BookingsPage.css";

function formatInstant(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

export default function BookingsPage() {
  const { user } = useAuth();
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<number | null>(null);

  useEffect(() => {
    if (!user?.userId) { setLoading(false); return; }
    reservationsApi.getByUser(user.userId)
      .then(setReservations)
      .catch(err => setError(err instanceof Error ? err.message : "Failed to load bookings"))
      .finally(() => setLoading(false));
  }, [user?.userId]);

  async function handleCancel(reservationId: number) {
    setCancelling(reservationId);
    try {
      const updated = await reservationsApi.cancel(reservationId);
      setReservations(prev =>
        prev.map(r => r.reservationId === reservationId ? { ...r, status: updated.status } : r)
      );
    } catch (err) {
      alert(err instanceof Error ? err.message : "Cancel failed.");
    } finally {
      setCancelling(null);
    }
  }

  if (loading) return <div style={{ padding: "2rem", textAlign: "center" }}>Loading bookings…</div>;
  if (error) return <div style={{ padding: "2rem", textAlign: "center", color: "red" }}>{error}</div>;

  return (
    <div className="bookings-page">
      <div className="bookings-header">
        <h1 className="bookings-title">My Bookings</h1>
        <p className="bookings-subtitle">Your room reservations, most recent first.</p>
      </div>

      {reservations.length === 0 ? (
        <div className="bookings-empty">
          <div className="bookings-empty__icon">📅</div>
          <div>No reservations yet. Head to Study Rooms to book one.</div>
        </div>
      ) : (
        <ul className="bookings-list">
          {reservations.map(r => (
            <li
              key={r.reservationId}
              className={`booking-card${r.status === "CANCELLED" ? " booking-card--cancelled" : ""}`}
            >
              <div className="booking-card__info">
                <p className="booking-card__room">
                  {r.buildingName} {r.roomNumber}
                </p>
                <p className="booking-card__time">
                  {formatInstant(r.startTime)} — {formatInstant(r.endTime)}
                </p>
                <span className={`booking-card__status booking-card__status--${r.status.toLowerCase()}`}>
                  {r.status}
                </span>
              </div>
              <button
                className="booking-card__cancel"
                disabled={r.status !== "CONFIRMED" || cancelling === r.reservationId}
                onClick={() => handleCancel(r.reservationId)}
              >
                {cancelling === r.reservationId ? "Cancelling…" : "Cancel"}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
