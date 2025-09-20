import NextAuth, { NextAuthOptions } from 'next-auth';
import Credentials from 'next-auth/providers/credentials';
import Keycloak from 'next-auth/providers/keycloak';

const authOptions: NextAuthOptions = {
  providers: [
    // Real OIDC via Keycloak-compatible provider if env is set
    ...(process.env.OIDC_ISSUER && process.env.OIDC_CLIENT_ID && process.env.OIDC_CLIENT_SECRET
      ? [
          Keycloak({
            issuer: process.env.OIDC_ISSUER,
            clientId: process.env.OIDC_CLIENT_ID,
            clientSecret: process.env.OIDC_CLIENT_SECRET,
          }),
        ]
      : []),
    // Credentials fallback for local dev/E2E testing
    Credentials({
      name: 'Credentials',
      credentials: {
        username: { label: 'Username', type: 'text' },
        password: { label: 'Password', type: 'password' },
      },
      async authorize(credentials) {
        // Stubbed authorize: accept any non-empty username
        if (credentials?.username) {
          return {
            id: 'dev-user',
            name: credentials.username,
            email: `${credentials.username}@example.com`,
          } as { id: string; name: string; email: string };
        }
        return null;
      },
    }),
  ],
  session: {
    strategy: 'jwt',
  },
  cookies: {
    sessionToken: {
      name: '__Host-next-auth.session-token',
      options: {
        httpOnly: true,
        sameSite: 'lax',
        path: '/',
        secure: process.env.NODE_ENV === 'production',
      },
    },
  },
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.name = user.name ?? token.name;
        token.email = (user as { email?: string | null })?.email ?? token.email;
        // token.sub is user id implicitly
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.name = (token as { name?: string | null }).name ?? session.user.name ?? null;
        session.user.email = (token as { email?: string | null }).email ?? session.user.email ?? null;
      }
      return session;
    },
  },
};

const handler = NextAuth(authOptions);
export { handler as GET, handler as POST };
