'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useSession } from 'next-auth/react';

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { status } = useSession();

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/');
    }
  }, [status, router]);

  if (status === 'loading') {
    return <div className="p-4 text-sm text-gray-500">Checking authentication…</div>;
  }

  if (status === 'authenticated') {
    return <div>{children}</div>;
  }

  // unauthenticated: render nothing while redirecting
  return null;
}
