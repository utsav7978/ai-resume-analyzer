import axiosInstance from './axiosInstance';

export const adminApi = {
  getDashboard:      ()         => axiosInstance.get('/admin/dashboard'),
  getAllUsers:        ()         => axiosInstance.get('/admin/users'),
  getUserDetails:    (userId)   => axiosInstance.get(`/admin/users/${userId}`),
  deleteUser:        (userId)   => axiosInstance.delete(`/admin/users/${userId}`),
  getAllResumes:      ()         => axiosInstance.get('/admin/resumes'),
  getUserResumes:    (userId)   => axiosInstance.get(`/admin/users/${userId}/resumes`),
  deleteResume:      (resumeId) => axiosInstance.delete(`/admin/resume/${resumeId}`),
  getResumeAnalysis: (resumeId) => axiosInstance.get(`/admin/resume/${resumeId}/analysis`),
};