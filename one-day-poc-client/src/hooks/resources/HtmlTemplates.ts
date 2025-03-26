export type TemplateType = 'react' | 'javascript' | 'fallback';

/**
 * HTML templates for various project types
 */
export const htmlTemplates = {
  react: `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>React App</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="{{ENTRY_POINT}}"></script>
  </body>
</html>`,

  javascript: `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>JavaScript App</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="{{ENTRY_POINT}}"></script>
  </body>
</html>`,

  fallback: `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Web Application</title>
  </head>
  <body>
    <div id="root"></div>
    <div id="app"></div>
    <!-- No entry point found. Common entry points are:
      - src/main.jsx, src/main.tsx, src/index.jsx, src/index.tsx
      - src/App.jsx, src/App.tsx, main.js, index.js
    -->
    <h1>No Entry Point Found</h1>
    <p>Please create one of the standard entry point files or update this HTML to point to your code.</p>
  </body>
</html>`
};

/**
 * Common entry points to search for
 */
export const entryPoints: Array<{path: string, type: TemplateType}> = [
  { path: '/src/main.jsx', type: 'react' },
  { path: '/src/main.tsx', type: 'react' },
  { path: '/src/index.jsx', type: 'react' },
  { path: '/src/index.tsx', type: 'react' },
  { path: '/src/App.jsx', type: 'react' },
  { path: '/src/App.tsx', type: 'react' },
  { path: '/main.jsx', type: 'react' },
  { path: '/main.tsx', type: 'react' },
  { path: '/index.jsx', type: 'react' },
  { path: '/index.tsx', type: 'react' },
  { path: '/src/main.js', type: 'javascript' },
  { path: '/src/index.js', type: 'javascript' },
  { path: '/main.js', type: 'javascript' },
  { path: '/index.js', type: 'javascript' }
];