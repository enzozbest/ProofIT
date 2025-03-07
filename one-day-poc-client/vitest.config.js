import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from "path";
import {fileURLToPath} from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            "@": path.resolve(__dirname, "src"),
        },
    },
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: './src/setupTests.js',
        moduleNameMapper: {
            '\\.(svg)$': '<rootDir>/src/__mocks__/fileMock.ts'
        },
        coverage: {
            reporter: ['text', 'html', 'lcov'],
            exclude:[
                'postcss.config.js',
                'tailwind.config.js',
                '.eslintrc.js',
                'eslint.config.js',
                'vite.config.js',
                'vitest.config.js',
                'src/__tests__',
                'src/components/ui'
            ]
        },
    },
});