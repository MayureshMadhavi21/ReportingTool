import axios from 'axios';

export const reportApi = axios.create({
  baseURL: 'http://localhost:8084/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const connectorApi = axios.create({
  baseURL: 'http://localhost:8085/api',
  headers: {
    'Content-Type': 'application/json',
  },
});
