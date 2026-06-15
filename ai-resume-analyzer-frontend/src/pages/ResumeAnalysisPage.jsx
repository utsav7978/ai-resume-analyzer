import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { analysisApi } from '../api/analysisApi';
import { resumeApi } from '../api/resumeApi';
import Navbar from '../components/layout/Navbar';
import LoadingSpinner from '../components/ui/LoadingSpinner';
import ErrorAlert from '../components/ui/ErrorAlert';

export default function ResumeAnalysisPage() {
  const { resumeId } = useParams();
  const navigate     = useNavigate();

  const [analysis, setAnalysis]   = useState(null);
  const [resume, setResume]       = useState(null);
  const [loading, setLoading]     = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError]         = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      const resumeRes = await resumeApi.getById(resumeId);
      setResume(resumeRes.data.data);

      // If analysis exists, fetch it
      if (resumeRes.data.data.analysisAvailable) {
        const analysisRes = await analysisApi.getByResume(resumeId);
        setAnalysis(analysisRes.data.data);
      }
    } catch {
      setError('Failed to load analysis data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, [resumeId]);

  const handleAnalyze = async () => {
    try {
      setAnalyzing(true);
      setError('');
      const res = await analysisApi.analyze(resumeId);
      setAnalysis(res.data.data);
      setResume(prev => ({ ...prev, analysisAvailable: true,
                            analysisStatus: 'COMPLETED' }));
    } catch (err) {
      setError(err.response?.data?.message || 'Analysis failed.');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleReAnalyze = async () => {
    try {
      setAnalyzing(true);
      setError('');
      const res = await analysisApi.reAnalyze(resumeId);
      setAnalysis(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Re-analysis failed.');
    } finally {
      setAnalyzing(false);
    }
  };

  const getScoreColor = (score) => {
    if (score >= 75) return 'text-green-600';
    if (score >= 50) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getScoreBg = (score) => {
    if (score >= 75) return 'bg-green-50 border-green-200';
    if (score >= 50) return 'bg-yellow-50 border-yellow-200';
    return 'bg-red-50 border-red-200';
  };

  if (loading) return <><Navbar /><LoadingSpinner /></>;

  return (
    <>
      <Navbar />
      <div className="max-w-5xl mx-auto px-4 py-8">

        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <button
              onClick={() => navigate('/dashboard')}
              className="text-sm text-primary-600 hover:text-primary-700
                         font-medium mb-2 flex items-center gap-1"
            >
              ← Back to Dashboard
            </button>
            <h1 className="text-2xl font-bold text-gray-900">
              Resume Analysis
            </h1>
            {resume && (
              <p className="text-gray-500 mt-1">{resume.fileName}</p>
            )}
          </div>

          {analysis && (
            <button
              onClick={handleReAnalyze}
              disabled={analyzing}
              className="btn-secondary text-sm"
            >
              {analyzing ? 'Re-analyzing...' : '🔄 Re-analyze'}
            </button>
          )}
        </div>

        <ErrorAlert message={error} onDismiss={() => setError('')} />

        {/* Not yet analyzed */}
        {!analysis && !analyzing && (
          <div className="card text-center py-16">
            <div className="text-5xl mb-4">🤖</div>
            <h2 className="text-xl font-semibold text-gray-800 mb-2">
              Ready to Analyze
            </h2>
            <p className="text-gray-500 mb-6">
              Click below to start AI analysis of your resume
            </p>
            <button onClick={handleAnalyze} className="btn-primary px-8">
              Start Analysis
            </button>
          </div>
        )}

        {/* Analyzing spinner */}
        {analyzing && (
          <div className="card text-center py-16">
            <div className="h-12 w-12 animate-spin rounded-full border-4
                            border-primary-200 border-t-primary-600 mx-auto mb-4" />
            <p className="text-gray-600 font-medium">
              AI is analyzing your resume...
            </p>
            <p className="text-gray-400 text-sm mt-1">
              This usually takes 5–10 seconds
            </p>
          </div>
        )}

        {/* Analysis Results */}
        {analysis && !analyzing && (
          <div className="space-y-6">

            {/* Score Card */}
            <div className={`card border-2 ${getScoreBg(analysis.overallScore)}`}>
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-semibold text-gray-800">
                    Overall Resume Score
                  </h2>
                  <p className="text-gray-500 text-sm mt-1">
                    Analyzed by {analysis.groqModelUsed} ·{' '}
                    {new Date(analysis.analyzedAt).toLocaleString()}
                  </p>
                </div>
                <div className={`text-6xl font-bold ${getScoreColor(analysis.overallScore)}`}>
                  {analysis.overallScore}
                  <span className="text-2xl text-gray-400">/100</span>
                </div>
              </div>
              {/* Score bar */}
              <div className="mt-4 bg-gray-200 rounded-full h-3">
                <div
                  className={`h-3 rounded-full transition-all duration-1000
                    ${analysis.overallScore >= 75 ? 'bg-green-500' :
                      analysis.overallScore >= 50 ? 'bg-yellow-500' : 'bg-red-500'}`}
                  style={{ width: `${analysis.overallScore}%` }}
                />
              </div>
            </div>

            {/* Two column grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <SkillsCard
                title="Technical Skills"
                icon="💻"
                items={analysis.technicalSkills}
                tagColor="bg-primary-50 text-primary-700 border border-primary-100"
              />
              <SkillsCard
                title="Soft Skills"
                icon="🤝"
                items={analysis.softSkills}
                tagColor="bg-purple-50 text-purple-700 border border-purple-100"
              />
              <ListCard
                title="Strengths"
                icon="💪"
                items={analysis.strengths}
                iconColor="text-green-500"
                bulletColor="text-green-500"
              />
              <ListCard
                title="Weaknesses / Gaps"
                icon="📉"
                items={analysis.weaknesses}
                iconColor="text-red-500"
                bulletColor="text-red-400"
              />
              <ListCard
                title="Recommended Job Roles"
                icon="🎯"
                items={analysis.recommendedRoles}
                iconColor="text-blue-500"
                bulletColor="text-blue-400"
              />
              <SkillsCard
                title="Missing Skills to Add"
                icon="➕"
                items={analysis.missingSkills}
                tagColor="bg-orange-50 text-orange-700 border border-orange-100"
              />
            </div>

          </div>
        )}

      </div>
    </>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function SkillsCard({ title, icon, items, tagColor }) {
  return (
    <div className="card">
      <h3 className="font-semibold text-gray-800 mb-3 flex items-center gap-2">
        <span>{icon}</span>{title}
        <span className="ml-auto text-xs text-gray-400 font-normal">
          {items?.length ?? 0} items
        </span>
      </h3>
      {items?.length > 0 ? (
        <div className="flex flex-wrap gap-2">
          {items.map(item => (
            <span key={item}
                  className={`px-3 py-1 rounded-full text-sm font-medium ${tagColor}`}>
              {item}
            </span>
          ))}
        </div>
      ) : (
        <p className="text-gray-400 text-sm italic">None detected</p>
      )}
    </div>
  );
}

function ListCard({ title, icon, items, bulletColor }) {
  return (
    <div className="card">
      <h3 className="font-semibold text-gray-800 mb-3 flex items-center gap-2">
        <span>{icon}</span>{title}
        <span className="ml-auto text-xs text-gray-400 font-normal">
          {items?.length ?? 0} items
        </span>
      </h3>
      {items?.length > 0 ? (
        <ul className="space-y-2">
          {items.map((item, i) => (
            <li key={i} className="flex items-start gap-2 text-sm text-gray-700">
              <span className={`${bulletColor} mt-0.5 flex-shrink-0`}>▸</span>
              {item}
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-gray-400 text-sm italic">None detected</p>
      )}
    </div>
  );
}