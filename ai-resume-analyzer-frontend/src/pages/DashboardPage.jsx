import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api/userApi';
import { resumeApi } from '../api/resumeApi';
import Navbar from '../components/layout/Navbar';
import LoadingSpinner from '../components/ui/LoadingSpinner';
import ErrorAlert from '../components/ui/ErrorAlert';
import ConfirmModal from '../components/ui/ConfirmModal';

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');
  const [deleteModal, setDeleteModal] = useState({ open: false, resumeId: null });

  const navigate = useNavigate();

  const fetchDashboard = async () => {
    try {
      setLoading(true);
      const res = await userApi.getDashboard();
      setDashboard(res.data.data);
    } catch {
      setError('Failed to load dashboard.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchDashboard(); }, []);

  const handleDelete = async () => {
    try {
      await resumeApi.delete(deleteModal.resumeId);
      setDeleteModal({ open: false, resumeId: null });
      fetchDashboard();
    } catch {
      setError('Failed to delete resume.');
    }
  };

  const getStatusBadge = (status) => {
    const map = {
      COMPLETED:   'badge-green',
      PENDING:     'badge-yellow',
      PROCESSING:  'badge-blue',
      FAILED:      'badge-red',
      PARSE_FAILED:'badge-red',
    };
    return map[status] || 'badge-gray';
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return 'N/A';
    return (bytes / 1024).toFixed(1) + ' KB';
  };

  if (loading) return <><Navbar /><LoadingSpinner /></>;

  return (
    <>
      <Navbar />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Welcome back, {dashboard?.profile?.name?.split(' ')[0]} 👋
            </h1>
            <p className="text-gray-500 mt-1">
              Here's an overview of your resume analyses
            </p>
          </div>
          <button
            onClick={() => navigate('/upload')}
            className="btn-primary"
          >
            + Upload Resume
          </button>
        </div>

        <ErrorAlert message={error} onDismiss={() => setError('')} />

        {/* Stats Row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatCard
            icon="📄"
            label="Total Resumes"
            value={dashboard?.stats?.totalResumes ?? 0}
            color="blue"
          />
          <StatCard
            icon="🤖"
            label="Analyses Done"
            value={dashboard?.stats?.totalAnalyses ?? 0}
            color="purple"
          />
          <StatCard
            icon="🏆"
            label="Highest Score"
            value={dashboard?.stats?.highestScore != null
              ? `${dashboard.stats.highestScore}/100` : '—'}
            color="green"
          />
          <StatCard
            icon="📊"
            label="Average Score"
            value={dashboard?.stats?.averageScore != null
              ? `${dashboard.stats.averageScore}/100` : '—'}
            color="orange"
          />
        </div>

        {/* Resumes List */}
        <div className="card">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">
            Your Resumes
          </h2>

          {dashboard?.resumes?.length === 0 ? (
            <div className="text-center py-16">
              <div className="text-5xl mb-4">📂</div>
              <p className="text-gray-500 font-medium">No resumes uploaded yet</p>
              <p className="text-gray-400 text-sm mt-1 mb-6">
                Upload your first resume to get AI-powered analysis
              </p>
              <button
                onClick={() => navigate('/upload')}
                className="btn-primary"
              >
                Upload Your First Resume
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {dashboard?.resumes?.map(resume => (
                <div key={resume.id}
                     className="border border-gray-100 rounded-xl p-5
                                hover:border-primary-200 hover:bg-primary-50/30
                                transition-all duration-200">
                  <div className="flex items-start justify-between gap-4">

                    {/* Left: file info */}
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="text-3xl flex-shrink-0">📑</div>
                      <div className="min-w-0">
                        <p className="font-medium text-gray-900 truncate">
                          {resume.fileName}
                        </p>
                        <p className="text-sm text-gray-400 mt-0.5">
                          {formatFileSize(resume.fileSize)} ·{' '}
                          {new Date(resume.uploadedAt).toLocaleDateString()}
                        </p>
                        <div className="flex items-center gap-2 mt-2">
                          <span className={getStatusBadge(resume.analysisStatus)}>
                            {resume.analysisStatus}
                          </span>
                          {resume.overallScore != null && (
                            <span className="badge badge-green">
                              Score: {resume.overallScore}/100
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Right: actions */}
                    <div className="flex items-center gap-2 flex-shrink-0">
                      {resume.analysisAvailable ? (
                        <button
                          onClick={() => navigate(`/analysis/${resume.id}`)}
                          className="btn-primary text-sm py-2 px-4"
                        >
                          View Analysis
                        </button>
                      ) : resume.analysisStatus === 'PENDING' ? (
                        <button
                          onClick={() => navigate(`/analysis/${resume.id}`)}
                          className="btn-secondary text-sm py-2 px-4"
                        >
                          Analyze Now
                        </button>
                      ) : null}
                      <button
                        onClick={() => setDeleteModal({
                          open: true, resumeId: resume.id
                        })}
                        className="p-2 text-gray-400 hover:text-red-500
                                   hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete resume"
                      >
                        🗑
                      </button>
                    </div>

                  </div>

                  {/* Skills preview */}
                  {resume.technicalSkills?.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {resume.technicalSkills.slice(0, 6).map(skill => (
                        <span key={skill}
                              className="px-2 py-0.5 bg-primary-50 text-primary-700
                                         text-xs rounded-md font-medium">
                          {skill}
                        </span>
                      ))}
                      {resume.technicalSkills.length > 6 && (
                        <span className="px-2 py-0.5 bg-gray-100 text-gray-500
                                         text-xs rounded-md">
                          +{resume.technicalSkills.length - 6} more
                        </span>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

      </div>

      <ConfirmModal
        isOpen={deleteModal.open}
        title="Delete Resume"
        message="This will permanently delete the resume and its analysis. This action cannot be undone."
        confirmText="Delete"
        onConfirm={handleDelete}
        onCancel={() => setDeleteModal({ open: false, resumeId: null })}
      />
    </>
  );
}

function StatCard({ icon, label, value, color }) {
  const colors = {
    blue:   'bg-blue-50 text-blue-600',
    purple: 'bg-purple-50 text-purple-600',
    green:  'bg-green-50 text-green-600',
    orange: 'bg-orange-50 text-orange-600',
  };
  return (
    <div className="card flex items-center gap-4">
      <div className={`${colors[color]} p-3 rounded-xl text-2xl`}>
        {icon}
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
      </div>
    </div>
  );
}