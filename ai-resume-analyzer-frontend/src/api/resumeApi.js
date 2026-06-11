import axiosInstance from './axiosInstance';

export const resumeApi = {
  upload:      (formData) => axiosInstance.post('/resume/upload', formData, {
                  headers: { 'Content-Type': 'multipart/form-data' },
               }),
  getMyResumes: ()         => axiosInstance.get('/resume/my-resumes'),
  getById:     (id)        => axiosInstance.get(`/resume/${id}`),
  delete:      (id)        => axiosInstance.delete(`/resume/${id}`),
  textPreview: (id)        => axiosInstance.get(`/resume/${id}/text-preview`),
};