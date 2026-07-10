// src/lib/api.js
import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const isNgrokApi = API_URL.includes('ngrok-free.');
const api = axios.create({
    baseURL: API_URL,
    withCredentials: true,
});

api.interceptors.request.use((config) => {
    config.headers = config.headers || {};
    if (isNgrokApi) {
        config.headers['ngrok-skip-browser-warning'] = 'true';
    }

    // if this is an auth endpoint (login or register), don’t send any old JWT
    if (config.url?.startsWith('/auth/')) {
        delete config.headers?.Authorization;
        return config;
    }

    // otherwise attach the current token
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    } else {
        delete config.headers?.Authorization;
    }
    return config;
});

export default api;
