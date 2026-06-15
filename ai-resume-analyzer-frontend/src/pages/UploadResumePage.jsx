import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { resumeApi } from '../api/resumeApi';
import { analysisApi } from '../api/analysisApi';
import Navbar from '../components/layout/Navbar';
import ErrorAlert from '../components/ui/ErrorAlert';

export default function UploadResumePage() {
  const [file, setFile]           = useState(null);
  const [dragOver, setDragOver]   = useState(false);
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError]         = useState('');
  const [step, setStep]           = useState('select'); // select | uploading | analyzing | done

  const fileInputRef = useRef(null);
  const navigate     = useNavigate();

  const handleFileSelect = (selectedFile) => {
    if (!selectedFile) return;
    if (selectedFile.type !== 'application/pdf') {
      setError('Only PDF files are allowed.');
      return;
    }
    if (selectedFile.size > 5 * 1024 * 1024) {
      setError('File size must be under 5MB.');
      return;
    }
    setFile(selectedFile);
    setError('');
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const dropped = e.dataTransfer.files[0];
    handleFileSelect(dropped);
  };

  const handleUploadAndAnalyze = async () => {
    if (!file) return;
    setError('');

    try {
      // Step 1: Upload
      setStep('uploading');
      setUploading(true);
      const formData = new FormData();
      formData.append('file', file);
      const uploadRes = await resumeApi.upload(formData);
      const resumeId = uploadRes.data.data.id;
      setUploading(false);

      // Step 2: Analyze
      setStep('analyzing');
      setAnalyzing(true);
      await analysisApi.analyze(resumeId);
      setAnalyzing(false);

      // Step 3: Navigate to results
      setStep('done');
      setTimeout(() => navigate(`/analysis/${resumeId}`), 800);

    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed. Please try again.');
      setStep('select');
      setUploading(false);
      setAnalyzing(false);
    }
  };

  const formatSize = (bytes) => (bytes / 1024).toFixed(1) + ' KB';

  return (
    <>
      <Navbar />
      <div className="max-w-2xl mx-auto px-4 py-10">

        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900">Upload Resume</h1>
          <p className="text-gray-500 mt-1">
            Upload your PDF resume and get instant AI analysis
          </p>
        </div>

        <ErrorAlert message={error} onDismiss={() => setError('')} />

        {/* Progress Steps */}
        <div className="flex items-center gap-2 mb-8">
          {[
            { key: 'select',    label: '1. Select File' },
            { key: 'uploading', label: '2. Uploading'   },
            { key: 'analyzing', label: '3. Analyzing'   },
            { key: 'done',      label: '4. Done'        },
          ].map((s, i, arr) => (
            <div key={s.key} className="flex items-center gap-2">
              <div className={`
                px-3 py-1 rounded-full text-xs font-medium transition-colors
                ${step === s.key
                  ? 'bg-primary-600 text-white'
                  : ['uploading','analyzing','done'].indexOf(step) >
                    ['uploading','analyzing','done'].indexOf(s.key)
                    ? 'bg-green-100 text-green-700'
                    : 'bg-gray-100 text-gray-400'}
              `}>
                {s.label}
              </div>
              {i < arr.length - 1 && (
                <div className="w-4 h-px bg-gray-200" />
              )}
            </div>
          ))}
        </div>

        {/* Drop Zone */}
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => !file && fileInputRef.current?.click()}
          className={`
            border-2 border-dashed rounded-xl p-12 text-center
            transition-all duration-200 cursor-pointer
            ${dragOver
              ? 'border-primary-400 bg-primary-50'
              : file
              ? 'border-green-300 bg-green-50 cursor-default'
              : 'border-gray-300 hover:border-primary-300 hover:bg-gray-50'}
          `}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf"
            className="hidden"
            onChange={(e) => handleFileSelect(e.target.files[0])}
          />

          {file ? (
            <div>
              <div className="text-5xl mb-3">✅</div>
              <p className="font-semibold text-gray-900">{file.name}</p>
              <p className="text-sm text-gray-500 mt-1">{formatSize(file.size)}</p>
              <button
                onClick={(e) => { e.stopPropagation(); setFile(null); }}
                className="mt-3 text-sm text-red-500 hover:text-red-700"
              >
                Remove file
              </button>
            </div>
          ) : (
            <div>
              <div className="text-5xl mb-3">📂</div>
              <p className="font-semibold text-gray-700">
                Drop your PDF here or click to browse
              </p>
              <p className="text-sm text-gray-400 mt-1">
                PDF only · Max 5MB
              </p>
            </div>
          )}
        </div>

        {/* Status Messages */}
        {step === 'uploading' && (
          <div className="mt-6 flex items-center gap-3 p-4 bg-blue-50
                          border border-blue-200 rounded-lg">
            <div className="h-5 w-5 animate-spin rounded-full
                            border-2 border-blue-200 border-t-blue-600" />
            <p className="text-blue-700 font-medium">
              Uploading and extracting text from PDF...
            </p>
          </div>
        )}

        {step === 'analyzing' && (
          <div className="mt-6 flex items-center gap-3 p-4 bg-purple-50
                          border border-purple-200 rounded-lg">
            <div className="h-5 w-5 animate-spin rounded-full
                            border-2 border-purple-200 border-t-purple-600" />
            <p className="text-purple-700 font-medium">
              AI is analyzing your resume... This takes 5–10 seconds.
            </p>
          </div>
        )}

        {step === 'done' && (
          <div className="mt-6 flex items-center gap-3 p-4 bg-green-50
                          border border-green-200 rounded-lg">
            <span className="text-green-600 text-xl">✅</span>
            <p className="text-green-700 font-medium">
              Analysis complete! Redirecting to results...
            </p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3 mt-6">
          <button
            onClick={() => navigate('/dashboard')}
            className="btn-secondary flex-1"
          >
            Cancel
          </button>
          <button
            onClick={handleUploadAndAnalyze}
            disabled={!file || step !== 'select'}
            className="btn-primary flex-1"
          >
            {step === 'select' ? 'Upload & Analyze' : 'Processing...'}
          </button>
        </div>

        {/* Info Box */}
        <div className="mt-8 p-4 bg-gray-50 rounded-xl border border-gray-100">
          <p className="text-sm font-medium text-gray-700 mb-2">
            What happens after upload:
          </p>
          <ul className="space-y-1">
            {[
              '📄 PDF text is extracted using Apache PDFBox',
              '🤖 Groq AI (Llama 3.1) analyzes your resume',
              '💡 Get skills, strengths, weaknesses & job suggestions',
              '📊 Receive an overall resume score out of 100',
            ].map(item => (
              <li key={item} className="text-sm text-gray-500">{item}</li>
            ))}
          </ul>
        </div>

      </div>
    </>
  );
}