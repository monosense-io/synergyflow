# ADR-0005: Shadcn/ui over Chakra UI or MUI

**Status**: Accepted
**Date**: 2025-10-18
**Author**: Winston (Architect)
**Deciders**: Product Owner, Frontend Team

---

## Context

SynergyFlow frontend requires a component library with:
- **Accessible components** (WCAG 2.1 AA compliant)
- **Customizable styling** (match brand colors, design tokens)
- **TypeScript support** (full type safety)
- **Tailwind CSS integration** (utility-first CSS)
- **Production-ready components** (Button, Input, Select, Dialog, etc.)

We needed to choose:
1. **Shadcn/ui + Radix UI** (headless + Tailwind)
2. **Chakra UI** (all-in-one component library)
3. **Material-UI (MUI)** (Material Design components)
4. **Ant Design** (enterprise component library)

Key constraints:
- Must integrate with Tailwind CSS (already chosen for utility-first approach)
- Full customization control (no vendor lock-in)
- WCAG 2.1 AA accessibility required
- 1 frontend developer (needs rapid development)

---

## Decision

**We will use Shadcn/ui + Radix UI + Tailwind CSS, NOT Chakra UI or MUI.**

### Rationale

**1. Copy-Paste Approach (Full Ownership, Zero Lock-In)**
- **No npm dependency**: Components copied into `components/ui/` folder
- **Full customization**: Modify component code directly (no wrapper components)
- **No version lock-in**: Update only components you need, no breaking changes
- **Bundle size control**: Only include components you actually use

**2. Tailwind CSS Native (Consistent Styling)**
- **Same design tokens**: Components use Tailwind classes from `tailwind.config.js`
- **No CSS-in-JS overhead**: No runtime style calculation, no style tag injection
- **Utility-first**: Developers use familiar Tailwind patterns for customization
- **Design system integration**: Colors, spacing, typography from single source of truth

**3. Radix UI Primitives (Accessibility Built-In)**
- **WCAG 2.1 AA compliant**: Keyboard navigation, ARIA labels, focus management
- **Headless components**: Radix provides behavior, Shadcn provides styling
- **Battle-tested**: Used by Vercel, Linear, Cal.com, and other production apps
- **Composable**: Build complex components from primitives

**4. Developer Experience**
```bash
# Add component in 10 seconds
npx shadcn-ui@latest add button
npx shadcn-ui@latest add dialog
npx shadcn-ui@latest add select

# Customize immediately
// components/ui/button.tsx
<button className="bg-primary-500 hover:bg-primary-700 ...">
```

**5. Rapid Development + Full Control**
- **Pre-built components**: 50+ components (Button, Dialog, Table, etc.)
- **Customizable immediately**: Edit component code directly
- **No abstraction overhead**: No learning wrapper API, just Tailwind + React

---

## Consequences

### Positive

✅ **Full ownership**: Components in your codebase, modify freely
✅ **Zero lock-in**: No npm dependency bloat, no version hell
✅ **Tailwind native**: Consistent with utility-first CSS approach
✅ **Accessible**: Radix UI primitives WCAG 2.1 AA compliant
✅ **Rapid development**: 50+ pre-built components, add in seconds

### Negative

⚠️ **Manual updates**: Must manually copy new component versions (mitigated: stable components rarely need updates)
⚠️ **More files**: Components live in `components/ui/` folder (mitigated: organized structure)
⚠️ **Tailwind required**: Cannot use without Tailwind CSS (mitigated: already using Tailwind)

---

## Alternatives Considered

### Alternative 1: Chakra UI

**Rejected because**:
- **Npm dependency**: 800KB+ bundle, all components included
- **CSS-in-JS overhead**: Runtime style calculation, performance cost
- **Customization complexity**: Must use Chakra theme system, not Tailwind
- **Different mental model**: Chakra props API vs Tailwind utilities

**When to use**: Projects not using Tailwind, need rapid prototyping without customization.

### Alternative 2: Material-UI (MUI)

**Rejected because**:
- **Material Design opinionated**: Hard to deviate from Google's design language
- **Large bundle size**: 1MB+ for full library, tree-shaking helps but still large
- **Customization overhead**: Must override Material Design defaults extensively
- **CSS-in-JS**: Emotion/styled-components overhead

**When to use**: Enterprise apps requiring Material Design compliance, Google-like UI.

### Alternative 3: Ant Design

**Rejected because**:
- **Chinese design language**: Aesthetics may not fit Western markets
- **Less customizable**: Opinionated design decisions hard to override
- **Large bundle**: 1MB+ library, all components included
- **Less Tailwind integration**: Designed for Less/CSS Modules, not Tailwind

**When to use**: Enterprise apps in China/APAC region requiring Ant Design aesthetics.

---

## Validation

- **Bundle size**: Measure production bundle, target <200KB initial JS
- **Accessibility audit**: Run axe DevTools, WCAG 2.1 AA compliance
- **Developer velocity**: Track time to implement forms, modals, tables

---

## References

- **Shadcn/ui Documentation**: https://ui.shadcn.com/
- **Radix UI Documentation**: https://www.radix-ui.com/
- **Architecture Document**: [docs/architecture/18-ux-specification.md](../architecture/18-ux-specification.md)
- **Tech Stack**: [docs/architecture/3-tech-stack.md](../architecture/3-tech-stack.md)
