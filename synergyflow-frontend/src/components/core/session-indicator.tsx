'use client';

import { useSession, signIn, signOut } from 'next-auth/react';

export function SessionIndicator() {
  const { data: session, status } = useSession();

  if (status === 'loading') {
    return <span className="text-sm text-gray-500">Checking session…</span>;
  }

  if (session?.user) {
    return (
      <div className="flex items-center gap-3">
        <span className="text-sm">Signed in as {session.user.name ?? session.user.email}</span>
        <button className="text-sm underline" onClick={() => signOut({ callbackUrl: '/' })}>
          Sign out
        </button>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-gray-600 dark:text-gray-300">Signed out</span>
      <button className="text-sm underline" onClick={() => signIn()}>
        Sign in
      </button>
    </div>
  );
}
