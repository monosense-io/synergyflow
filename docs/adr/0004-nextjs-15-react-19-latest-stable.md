# ADR-0004: Next.js 15 & React 19 Latest Stable

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Frontend Team

---

## Context

SynergyFlow frontend requires a modern React framework with:
- **Server-side rendering (SSR)** for SEO and initial page load performance
- **App Router** for file-based routing and layouts
- **Server Components** for data fetching without client JS
- **TypeScript support** for type safety
- **Production-ready stability** (not experimental/beta)

We needed to choose:
1. **Next.js 15 (latest stable)** + React 19
2. **Next.js 14** + React 18 (previous stable)
3. **Remix** (alternative React framework)
4. **Vite + React Router** (SPA approach)

Key constraints:
- 12-month delivery timeline with 1 frontend developer
- Need for rapid development with best DX
- Production-ready stability (no beta/experimental features in critical path)

---

## Decision

**We will use Next.js 15.1+ with React 19 (latest stable releases), NOT Next.js 14 or alternative frameworks.**

### Rationale

**1. Performance Improvements (30% Faster Builds)**
- **Turbopack dev mode**: 30% faster local development builds
- **React 19 React Compiler**: Automatic memoization (no manual `useMemo`/`useCallback`)
- **Improved caching**: Better fetch cache defaults, automatic request deduplication
- **Partial Prerendering (experimental)**: Instant static shell + streaming dynamic content

**2. Developer Experience Enhancements**
- **Async Request APIs**: `params`, `searchParams` now async (cleaner API)
- **Better TypeScript support**: Improved type inference for Server Components
- **useFormStatus**: Built-in form pending states (no custom loading logic)
- **useOptimistic**: Optimistic UI updates without complex state management

**3. React 19 Features**
- **React Compiler**: Automatic memoization eliminates manual optimization
- **Improved Server Components**: Better streaming, error boundaries
- **Actions**: Server Actions for form submissions without API routes
- **ref as prop**: Cleaner component APIs (no forwardRef needed)

**4. Future-Proof**
- Latest stable releases (not experimental/beta)
- Backward compatibility maintained (incremental adoption)
- Active development and community support

**5. No Breaking Changes for SynergyFlow**
- Async Request APIs: Straightforward migration (add `await`)
- Form handling: Can use existing patterns or adopt new Actions
- Component code: Mostly unchanged (ref forwarding improved)

---

## Consequences

### Positive

✅ **Performance**: 30% faster builds (Turbopack), automatic memoization (React Compiler)
✅ **Developer experience**: Better TypeScript support, cleaner APIs (useFormStatus, useOptimistic)
✅ **Future-proof**: Latest stable releases, active development
✅ **React 19 features**: Server Actions, improved Server Components, ref as prop

### Negative

⚠️ **Async Request APIs**: `params`/`searchParams` require `await` (migration effort minimal)
⚠️ **Breaking changes**: Some packages may not support React 19 immediately (mitigated: major packages already updated)
⚠️ **Learning curve**: Team must learn new patterns (Actions, useOptimistic) (mitigated: opt-in adoption)

---

## Alternatives Considered

### Alternative 1: Next.js 14 + React 18

**Rejected because**: Missing Turbopack improvements, React Compiler, useFormStatus/useOptimistic hooks, no async Request APIs improvements.

**When to use**: Projects requiring absolute stability with no new features.

### Alternative 2: Remix

**Rejected because**: Smaller ecosystem, less mature tooling, team unfamiliar, no server components yet.

**When to use**: Teams preferring web standards (FormData, fetch), simpler mental model.

### Alternative 3: Vite + React Router (SPA)

**Rejected because**: No SSR out-of-box, manual setup for data fetching, SEO challenges, no server components.

**When to use**: Internal tools not needing SEO, simpler deployment (static hosting).

---

## Validation

- **Build performance**: Measure dev build time (target <5s for incremental builds)
- **Page load performance**: LCP <2.5s, FCP <1.8s (Core Web Vitals)
- **Developer productivity**: Sprint velocity tracking (story points per sprint)

---

## References

- **Next.js 15 Release Notes**: https://nextjs.org/blog/next-15
- **React 19 Release Notes**: https://react.dev/blog/2024/04/25/react-19
- **Architecture Document**: [docs/architecture/10-frontend-architecture.md](../architecture/10-frontend-architecture.md)
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md)
- **Detailed Rationale**: [docs/architecture/32-nextjs-15-react-19-why-latest-stable-versions.md](../architecture/32-nextjs-15-react-19-why-latest-stable-versions.md)
