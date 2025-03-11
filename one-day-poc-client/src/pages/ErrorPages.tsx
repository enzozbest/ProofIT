import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';

const ErrorPage: React.FC<{ code: string; message: string }> = ({
  code,
  message,
}) => {
  return (
    <div className="flex flex-col items-center justify-center h-screen text-white bg-gradient-to-br from-blue-900 to-purple-900">
      <h1 className="text-6xl font-bold">{code}</h1>
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

const ErrorRoutes = () => (
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
);

export default ErrorRoutes;
