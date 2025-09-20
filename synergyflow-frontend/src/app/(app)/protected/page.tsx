'use client';

import ProtectedRoute from '@/components/core/protected-route';

export default function ProtectedPage() {
  return (
    <ProtectedRoute>
      <div className="flex flex-col items-center justify-center min-h-screen py-2">
        <h1 className="text-4xl font-bold mb-4">Protected Page</h1>
        <p className="text-lg">This is a protected page that requires authentication.</p>
        <div className="mt-8 p-4 bg-yellow-100 dark:bg-yellow-900 rounded-lg">
          <p className="text-yellow-800 dark:text-yellow-200">
            Note: Authentication is currently stubbed. In a real implementation, 
            this page would only be accessible to authenticated users.
          </p>
        </div>
      </div>
    </ProtectedRoute>
  );
}