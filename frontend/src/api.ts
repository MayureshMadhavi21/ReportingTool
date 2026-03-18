import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8084/api', // Spring Boot default backend port
  headers: {
    'Content-Type': 'application/json',
  },
});

export default api;
