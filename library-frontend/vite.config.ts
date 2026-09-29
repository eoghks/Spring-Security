import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 서버: /api 요청은 백엔드(기본 8080)로 프록시한다 — 같은 출처로 보이므로 CORS 설정이 필요 없다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
});
