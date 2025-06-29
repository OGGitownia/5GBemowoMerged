import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

import svgr from 'vite-plugin-svgr'


export default defineConfig({
    base: '/',
    build: {
      outDir: 'dist'
    },
    plugins: [react(), svgr()],
    define: {
        global: 'window',
    },
    server: {
        proxy: {
            '/api': 'http://localhost:8080'
        }
    },
});

