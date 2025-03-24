import React from 'react';
import { Route, Routes } from 'react-router-dom';
import { ErrorBoundary } from 'react-error-boundary';

/**
 * Props for the ErrorPage component
 *
 * @property {string} message - The error message to display to the user
 * @property {string} [code] - Optional error code (e.g., "404", "500") to display prominently
 */
interface ErrorPageProps {
  message: string;
  code?: string;
}

/**
 * ErrorPage component renders a styled error page with optional error code
 *
 * Provides a consistent error page layout with the error code, message, and a
 * navigation link back to the home page.
 *
 * @component
 * @returns {JSX.Element} A styled error page with the provided information
 */
const ErrorPage: React.FC<ErrorPageProps> = ({ code, message }) => {
  return (
    <div className="flex flex-col items-center justify-center h-screen text-white bg-gradient-to-br from-blue-900 to-purple-900">
      {code && <h1 className="text-6xl font-bold">{code}</h1>}
      <p className="mt-4 text-xl">{message}</p>
      <a
        href="/"
        className="mt-6 px-6 py-3 bg-white text-gray-800 rounded-lg shadow-lg hover:bg-gray-200 transition"
      >
        Go Home
      </a>
    </div>
  );
};

/**
 * ErrorFallback component serves as the default error boundary fallback
 *
 * Used by the ErrorBoundary when an uncaught error occurs in the application
 * that isn't handled by one of the specific error routes.
 *
 * @returns {JSX.Element} A generic error page for unexpected errors
 */
const ErrorFallback = () => (
  <ErrorPage message="An unexpected error occurred. Please try again later." />
);

/**
 * ErrorRoutes component provides route definitions for different error conditions
 *
 * Sets up routes for common HTTP error codes (401, 403, 404, 500) and wraps them in
 * an ErrorBoundary to catch any rendering errors that might occur.
 *
 * @returns {JSX.Element} A collection of routes for different error pages
 */
const ErrorRoutes = () => (
  <ErrorBoundary FallbackComponent={ErrorFallback}>
    <Routes>
      <Route
        path="/403"
        element={
          <ErrorPage
            code="403"
            message="You don’t have permission to access this page."
          />
        }
      />
      <Route
        path="/401"
        element={
          <ErrorPage code="401" message="Please log in to access this page." />
        }
      />
      <Route
        path="/500"
        element={
          <ErrorPage code="500" message="Something went wrong on our end." />
        }
      />
      <Route
        path="*"
        element={
          <ErrorPage
            code="404"
            message="The page you are looking for does not exist."
          />
        }
      />
    </Routes>
  </ErrorBoundary>
);

export default ErrorRoutes;
