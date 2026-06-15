import { useState, useEffect } from 'react';
import { adminApi } from '../api/adminApi';
import Navbar from '../components/layout/Navbar';
import LoadingSpinner from '../components/ui/LoadingSpinner';
import ErrorAlert from '../components/ui/ErrorAlert';
import ConfirmModal from '../components/ui/ConfirmModal';

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [allUsers, setAllUsers]   = useState([]);
  const [allResumes, setAllResumes] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [deleteModal, setDeleteModal] = useState({
    open: false, type: null, id: null
  });

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const [dashRes, usersRes, resumesRes] = await Promise.all([
        adminApi.getDashboard(),
        adminApi.getAllUsers(),
        adminApi.getAllResumes(),
      ]);
      setDashboard(dashRes.data.data);
      setAllUsers(usersRes.data.data);
      setAllResumes(resumesRes.data.data);
    } catch {
      setError('Failed to load admin dashboard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadDashboard(); }, []);

  const handleDelete = async () => {
    try {
      if (deleteModal.type === 'user') {
        await adminApi.deleteUser(deleteModal.id);
      } else {
        await adminApi.deleteResume(deleteModal.id);
      }
      setDeleteModal({ open: false, type: null, id: null });
      loadDashboard();
    } catch {
      setError('Delete failed. Please try again.');
    }
  };

  const getStatusBadge = (status) => {
    const map = {
      COMPLETED:    'badge-green',
      PENDING:      'badge-yellow',
      PROCESSING:   'badge-blue',
      FAILED:       'badge-red',
      PARSE_FAILED: 'badge-red',
    };
    return map[status] || 'badge-gray';
  };

  if (loading) return <><Navbar /><LoadingSpinner /></>;

  const stats = dashboard?.stats;

  return (
    <>
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="text-gray-500 mt-1">Platform overview and management</p>
        </div>

        <ErrorAlert message={error} onDismiss={() => setError('')} />

        {/* Stats Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <AdminStatCard label="Total Users"
            value={stats?.totalUsers ?? 0} icon="👥" color="blue" />
          <AdminStatCard label="Total Resumes"
            value={stats?.totalResumes ?? 0} icon="📄" color="purple" />
          <AdminStatCard label="Completed Analyses"
            value={stats?.completedAnalyses ?? 0} icon="✅" color="green" />
          <AdminStatCard label="Avg Score"
            value={stats?.averageScore != null
              ? `${stats.averageScore}/100` : '—'}
            icon="📊" color="orange" />
        </div>

        {/* Secondary Stats */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <div className="card text-center">
            <p className="text-2xl font-bold text-yellow-600">
              {stats?.pendingAnalyses ?? 0}
            </p>
            <p className="text-sm text-gray-500 mt-1">Pending Analyses</p>
          </div>
          <div className="card text-center">
            <p className="text-2xl font-bold text-red-600">
              {stats?.failedAnalyses ?? 0}
            </p>
            <p className="text-sm text-gray-500 mt-1">Failed Analyses</p>
          </div>
          <div className="card text-center">
            <p className="text-2xl font-bold text-primary-600">
              {stats?.totalAnalyses ?? 0}
            </p>
            <p className="text-sm text-gray-500 mt-1">Total Analyses</p>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200 mb-6">
          <div className="flex gap-6">
            {[
              { key: 'overview', label: 'Overview'      },
              { key: 'users',    label: `Users (${allUsers.length})`   },
              { key: 'resumes',  label: `Resumes (${allResumes.length})` },
            ].map(tab => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`pb-3 text-sm font-medium border-b-2 transition-colors
                  ${activeTab === tab.key
                    ? 'border-primary-600 text-primary-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'}`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Recent Users */}
            <div className="card">
              <h2 className="font-semibold text-gray-800 mb-4">
                Recent Users
              </h2>
              <div className="space-y-3">
                {dashboard?.recentUsers?.map(user => (
                  <div key={user.id}
                       className="flex items-center justify-between
                                  p-3 bg-gray-50 rounded-lg">
                    <div>
                      <p className="font-medium text-sm text-gray-900">
                        {user.name}
                      </p>
                      <p className="text-xs text-gray-500">{user.email}</p>
                    </div>
                    <div className="text-right">
                      <span className={`badge ${
                        user.role === 'ROLE_ADMIN'
                          ? 'badge-blue' : 'badge-gray'}`}>
                        {user.role === 'ROLE_ADMIN' ? 'Admin' : 'User'}
                      </span>
                      <p className="text-xs text-gray-400 mt-1">
                        {user.resumeCount} resume(s)
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Recent Resumes */}
            <div className="card">
              <h2 className="font-semibold text-gray-800 mb-4">
                Recent Resumes
              </h2>
              <div className="space-y-3">
                {dashboard?.recentResumes?.map(resume => (
                  <div key={resume.id}
                       className="flex items-center justify-between
                                  p-3 bg-gray-50 rounded-lg">
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-sm text-gray-900 truncate">
                        {resume.fileName}
                      </p>
                      <p className="text-xs text-gray-500 truncate">
                        {resume.userEmail}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 ml-2 flex-shrink-0">
                      <span className={getStatusBadge(resume.analysisStatus)}>
                        {resume.analysisStatus}
                      </span>
                      {resume.overallScore != null && (
                        <span className="badge badge-green">
                          {resume.overallScore}/100
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Users Tab */}
        {activeTab === 'users' && (
          <div className="card overflow-hidden p-0">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  {['Name','Email','Role','Resumes','Joined','Actions']
                    .map(h => (
                    <th key={h}
                        className="px-4 py-3 text-left text-xs font-semibold
                                   text-gray-500 uppercase tracking-wider">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {allUsers.map(user => (
                  <tr key={user.id}
                      className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-medium text-gray-900">
                      {user.name}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{user.email}</td>
                    <td className="px-4 py-3">
                      <span className={`badge ${
                        user.role === 'ROLE_ADMIN'
                          ? 'badge-blue' : 'badge-gray'}`}>
                        {user.role === 'ROLE_ADMIN' ? 'Admin' : 'User'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {user.resumeCount ?? '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {user.createdAt
                        ? new Date(user.createdAt).toLocaleDateString()
                        : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setDeleteModal({
                          open: true, type: 'user', id: user.id
                        })}
                        className="text-red-500 hover:text-red-700
                                   hover:bg-red-50 px-2 py-1 rounded
                                   text-xs font-medium transition-colors"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Resumes Tab */}
        {activeTab === 'resumes' && (
          <div className="card overflow-hidden p-0">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-100">
                <tr>
                  {['File Name','User','Status','Score','Uploaded','Actions']
                    .map(h => (
                    <th key={h}
                        className="px-4 py-3 text-left text-xs font-semibold
                                   text-gray-500 uppercase tracking-wider">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {allResumes.map(resume => (
                  <tr key={resume.id}
                      className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-medium text-gray-900
                                   max-w-[180px] truncate">
                      {resume.fileName}
                    </td>
                    <td className="px-4 py-3 text-gray-500 max-w-[150px] truncate">
                      {resume.userEmail}
                    </td>
                    <td className="px-4 py-3">
                      <span className={getStatusBadge(resume.analysisStatus)}>
                        {resume.analysisStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-700 font-medium">
                      {resume.overallScore != null
                        ? `${resume.overallScore}/100` : '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {resume.uploadedAt
                        ? new Date(resume.uploadedAt).toLocaleDateString()
                        : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => setDeleteModal({
                          open: true, type: 'resume', id: resume.id
                        })}
                        className="text-red-500 hover:text-red-700
                                   hover:bg-red-50 px-2 py-1 rounded
                                   text-xs font-medium transition-colors"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

      </div>

      <ConfirmModal
        isOpen={deleteModal.open}
        title={deleteModal.type === 'user'
          ? 'Delete User' : 'Delete Resume'}
        message={deleteModal.type === 'user'
          ? 'This will permanently delete the user and ALL their resumes and analyses.'
          : 'This will permanently delete the resume and its analysis.'}
        confirmText="Delete"
        onConfirm={handleDelete}
        onCancel={() => setDeleteModal({ open: false, type: null, id: null })}
      />
    </>
  );
}

function AdminStatCard({ label, value, icon, color }) {
  const colors = {
    blue:   'bg-blue-50 text-blue-600',
    purple: 'bg-purple-50 text-purple-600',
    green:  'bg-green-50 text-green-600',
    orange: 'bg-orange-50 text-orange-600',
  };
  return (
    <div className="card flex items-center gap-4">
      <div className={`${colors[color]} p-3 rounded-xl text-2xl`}>{icon}</div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
      </div>
    </div>
  );
}