import axiosInstance from './axiosInstance';

export const userApi = {
  getDashboard: () => axiosInstance.get('/user/dashboard'),
  getProfile:   () => axiosInstance.get('/user/profile'),
};