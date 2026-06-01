import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

const Navbar = () => {
  const { name, isAuthenticated, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated) return null;

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <Link to="/kanban">📋 Task Manager</Link>
      </div>

      <div className="navbar-links">
        <Link to="/kanban" className="nav-link">
          Kanban
        </Link>
        <Link to="/dashboard" className="nav-link">
          Dashboard
        </Link>
      </div>

      <div className="navbar-user">
        <span className="user-name">Hola, {name}</span>
        <button onClick={handleLogout} className="logout-button">
          Cerrar Sesión
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
