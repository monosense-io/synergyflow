# SynergyFlow Frontend

IT Service Management Platform - Next.js 14 Frontend

## Technologies

- [Next.js 14](https://nextjs.org/) with App Router
- [TypeScript 5](https://www.typescriptlang.org/)
- [Tailwind CSS](https://tailwindcss.com/)
- [shadcn/ui](https://ui.shadcn.com/)
- [TanStack Query](https://tanstack.com/query)
- [NextAuth.js](https://next-auth.js.org/) (stubbed for OIDC)
- [Vitest](https://vitest.dev/) + [React Testing Library](https://testing-library.com/docs/react-testing-library/introduction/)
- [Playwright](https://playwright.dev/) for E2E testing

## Prerequisites

- Node.js 18+ (LTS recommended)
- npm, yarn, or pnpm

## Getting Started

1. Install dependencies:
   ```bash
   npm install
   ```

2. Copy the environment variables file and update the values:
   ```bash
   cp .env.example .env.local
   ```

3. Run the development server:
   ```bash
   npm run dev
   ```

4. Open [http://localhost:3000](http://localhost:3000) in your browser.

## Available Scripts

- `dev`: Run the development server
- `build`: Build the application for production
- `start`: Start the production server
- `lint`: Run ESLint
- `type-check`: Run TypeScript type checking
- `test`: Run unit tests with Vitest
- `test:watch`: Run unit tests in watch mode
- `e2e`: Run E2E tests with Playwright
- `e2e:ci`: Run E2E tests in CI mode
- `format`: Check code formatting with Prettier
- `format:write`: Fix code formatting with Prettier

## Project Structure

```
synergyflow-frontend/
  app/
    (app)/
      layout.tsx
      page.tsx
      protected/
        page.tsx
  components/
    ui/
    core/
  features/
    example/
      components/
      api/
  lib/
    api-client.ts
    auth.ts
    i18n.ts
    query-provider.tsx
  styles/
    globals.css
  public/
  e2e/
```

## Environment Variables

See `.env.example` for required environment variables:

- `NEXT_PUBLIC_API_BASE_URL` - Base URL for API calls
- `NEXTAUTH_URL` - Public app URL
- `NEXTAUTH_SECRET` - Secret for NextAuth
- `OIDC_ISSUER` - OIDC issuer URL
- `OIDC_CLIENT_ID` - OIDC client ID
- `OIDC_CLIENT_SECRET` - OIDC client secret
- `OIDC_WELLKNOWN` - Well-known discovery URL

## Testing

### Unit Tests

Run unit tests with Vitest:
```bash
npm run test
```

### E2E Tests

Run E2E tests with Playwright:
```bash
npm run e2e
```

For CI environments:
```bash
npm run e2e:ci
```

## Code Quality

- ESLint with recommended rules and accessibility plugins
- Prettier for code formatting
- TypeScript strict mode enabled

## Version Policy

- Track latest stable Next.js 14.x and TypeScript 5.x
- Monthly patch updates
- Minor updates after smoke suite
- Keep Playwright/Vitest aligned

## Features

- Dark/light theme toggle with system preference detection
- Protected route guard (stubbed)
- TanStack Query integration with error boundaries
- API client with interceptors and retry/backoff template
- i18n scaffolding with theme preference persistence

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.