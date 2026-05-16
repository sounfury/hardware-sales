import { defineConfig } from 'vite'
import path from 'path'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  plugins: [
    vue(),
    tailwindcss(),
    AutoImport({
      // Element Plus 样式已在 main.js 中全量引入，这里关闭按需样式注入，
      // 避免路由懒加载时再去动态拉取 *_style_css 模块导致白屏。
      resolvers: [ElementPlusResolver({ importStyle: false })],
      imports: ['vue', 'vue-router', 'pinia'],
    }),
    Components({
      // 仅保留组件自动注册，不重复注入按需样式。
      resolvers: [ElementPlusResolver({ importStyle: false })],
    }),
  ],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: ['test.sounfury.top'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
