/**
 * Configuration templates for different frameworks and tools
 */
export const configTemplates = {
  /**
   * Standard Vite configuration for React projects
   */
  viteReact: `
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react({
    // JSX runtime for React 17 compatibility when needed
    jsxRuntime: 'classic'
  })],
  server: {
    host: true,
    hmr: {
      clientPort: 443, // Fix for WebContainer environment
    }
  }
});
`,

  /**
   * Basic Vite configuration for JavaScript projects (no React)
   */
  viteJs: `
import { defineConfig } from 'vite';

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    host: true,
    hmr: {
      clientPort: 443, // Fix for WebContainer environment
    }
  }
});
`
};