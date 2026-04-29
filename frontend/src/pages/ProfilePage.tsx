import { useEffect, useState } from "react";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";
import "./ProfilePage.css";

export default function ProfilePage() {
  const { user } = useAuth();
  const [courseText, setCourseText] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [savedMsg, setSavedMsg] = useState("");

  useEffect(() => {
    if (!user?.userId) { setLoading(false); return; }
    api.get<{ userId: number; courseCodes: string[] }>(`/api/users/${user.userId}/courses`)
      .then(res => setCourseText(res.courseCodes.join(", ")))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user?.userId]);

  async function handleSave() {
    if (!user?.userId) return;
    const courseCodes = courseText
      .split(",")
      .map(s => s.trim())
      .filter(Boolean);
    setSaving(true);
    setSavedMsg("");
    try {
      await api.put(`/api/users/${user.userId}/courses`, { courseCodes });
      setSavedMsg("Saved!");
      setTimeout(() => setSavedMsg(""), 3000);
    } catch (err) {
      alert(err instanceof Error ? err.message : "Save failed.");
    } finally {
      setSaving(false);
    }
  }

  const previewCourses = courseText
    .split(",")
    .map(s => s.trim())
    .filter(Boolean);

  if (loading) return <div style={{ padding: "2rem", textAlign: "center" }}>Loading profile…</div>;

  return (
    <div className="profile-page">
      <div className="profile-header">
        <h1 className="profile-title">
          {user?.firstName ? `${user.firstName} ${user.lastName}` : user?.userName}
        </h1>
        <p className="profile-username">@{user?.userName}</p>
      </div>

      <div className="profile-section">
        <p className="profile-section__title">Enrolled Courses</p>
        <textarea
          className="profile-textarea"
          value={courseText}
          onChange={e => { setCourseText(e.target.value); setSavedMsg(""); }}
          placeholder="e.g. CSCI 201, CSCI 270, MATH 225"
          rows={3}
        />
        <p className="profile-section__hint">
          Separate courses with commas. Used for study partner matching.
        </p>
        {previewCourses.length > 0 && (
          <div className="profile-tags">
            {previewCourses.map(c => (
              <span key={c} className="profile-tag">{c}</span>
            ))}
          </div>
        )}
        <div className="profile-save-row">
          <button
            className="btn btn--primary btn--small"
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? "Saving…" : "Save courses"}
          </button>
          {savedMsg && <span className="profile-save-msg">{savedMsg}</span>}
        </div>
      </div>
    </div>
  );
}
