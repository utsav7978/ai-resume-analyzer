import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function Navbar() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">

          {/* Logo */}
          <Link to="/dashboard"
                className="flex items-center gap-2 font-bold text-xl
                           text-primary-600 hover:text-primary-700">
            <span className="text-2xl">📄</span>
            <span>ResumeAI</span>
          </Link>

          {/* Nav links */}
          <div className="hidden md:flex items-center gap-6">
            <Link to="/dashboard"
                  className="text-gray-600 hover:text-primary-600
                             font-medium transition-colors">
              Dashboard
            </Link>
            <Link to="/upload"
                  className="text-gray-600 hover:text-primary-600
                             font-medium transition-colors">
              Upload Resume
            </Link>
            {isAdmin() && (
              <Link to="/admin"
                    className="text-gray-600 hover:text-primary-600
                               font-medium transition-colors">
                Admin
              </Link>
            )}
          </div>

          {/* User menu */}
          <div className="flex items-center gap-4">
            <div className="hidden md:flex flex-col items-end">
              <span className="text-sm font-medium text-gray-900">
                {user?.name}
              </span>
              <span className="text-xs text-gray-500">{user?.email}</span>
            </div>
            <button
              onClick={handleLogout}
              className="btn-secondary text-sm py-2 px-4"
            >
              Logout
            </button>
          </div>

        </div>
      </div>
    </nav>
  );
}