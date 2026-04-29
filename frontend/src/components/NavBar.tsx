import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./NavBar.css";

export default function NavBar() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();

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
      </div>
      {user && (
        <span className="navbar__user">
          {user.guest ? "Guest" : user.firstName || user.userName}
        </span>
      )}
      <button className="navbar__logout" onClick={logout}>
        Log out
      </button>
    </nav>
  );
}
