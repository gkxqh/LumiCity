import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: false,
    proxy: {
      '/api': {
        // 默认指向本地 IDEA 运行的后端(8080)；可用 VITE_API_TARGET 覆盖（如指向 38080 的 jar）
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        ws: true          // 代理 WebSocket（/api/ws/alarm → 后端）
      }
    }
  }
})
