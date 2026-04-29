import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./NavBar.css";

export default function NavBar() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const isGuest = user?.guest ?? false;

  return (
    <nav className="navbar">
      <div className="navbar__brand" onClick={() => navigate("/rooms")} role="button" tabIndex={0}>
        <span className="navbar__logo" aria-hidden="true" />
        <span className="navbar__name">USC Study</span>
      </div>
      <div className="navbar__links">
        <NavLink to="/rooms" className={({ isActive }) => `navbar__link${isActive ? " navbar__link--active" : ""}`}>
          Study Rooms
        </NavLink>
        <NavLink to="/social" className={({ isActive }) => `navbar__link${isActive ? " navbar__link--active" : ""}`}>
          Study Partners
        </NavLink>
        {!isGuest && (
          <NavLink to="/bookings" className={({ isActive }) => `navbar__link${isActive ? " navbar__link--active" : ""}`}>
            My Bookings
          </NavLink>
        )}
      </div>
      {user && !isGuest && (
        <NavLink to="/profile" className={({ isActive }) => `navbar__user${isActive ? " navbar__user--active" : ""}`}>
          {user.firstName || user.userName}
        </NavLink>
      )}
      {user && isGuest && (
        <span className="navbar__user">Guest</span>
      )}
      <button className="navbar__logout" onClick={logout}>
        Log out
      </button>
    </nav>
  );
}
