import axiosInstance from './axiosInstance';

export const analysisApi = {
  analyze:      (resumeId) => axiosInstance.post(`/analysis/analyze/${resumeId}`),
  reAnalyze:    (resumeId) => axiosInstance.post(`/analysis/re-analyze/${resumeId}`),
  getByResume:  (resumeId) => axiosInstance.get(`/analysis/resume/${resumeId}`),
  getMyAll:     ()         => axiosInstance.get('/analysis/my-analyses'),
};