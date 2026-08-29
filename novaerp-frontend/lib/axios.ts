// lib/axios.ts
// Shared axios instance for the real API.

import axios from 'axios';
import { deleteCookie, getCookie } from '@/lib/cookies';

export const TOKEN_COOKIE_NAME = 'novaerp_token';
export const TOKEN_MAX_AGE_SECONDS = 60 * 60 * 24; // matches backend JWT_EXPIRATION_MS default (24h)

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = getCookie(TOKEN_COOKIE_NAME);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      typeof window !== 'undefined' &&
      error.response?.status === 401 &&
      !window.location.pathname.startsWith('/login')
    ) {
      deleteCookie(TOKEN_COOKIE_NAME);
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);
