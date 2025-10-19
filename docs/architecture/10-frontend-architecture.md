# 10. Frontend Architecture

## 10.1 Component Architecture

### Component Organization

SynergyFlow frontend follows Next.js 15 App Router structure with Server Components by default, Client Components where interactivity needed.

```
frontend/src/
├── app/                          # Next.js App Router pages (Server Components)
│   ├── (authenticated)/          # Route group for authenticated pages
│   │   ├── incidents/
│   │   │   ├── page.tsx          # Incident list page (SSR)
│   │   │   ├── [id]/
│   │   │   │   └── page.tsx      # Incident detail page (SSR)
│   │   │   └── new/
│   │   │       └── page.tsx      # Create incident page
│   │   ├── changes/
│   │   │   ├── page.tsx          # Change calendar page
│   │   │   └── [id]/page.tsx
│   │   ├── tasks/
│   │   │   ├── page.tsx          # Kanban board page
│   │   │   └── [id]/page.tsx
│   │   └── dashboard/
│   │       └── page.tsx          # Operations dashboard
│   ├── (public)/                 # Route group for public pages
│   │   ├── knowledge/
│   │   │   └── page.tsx          # Knowledge base search (SSG)
│   │   └── login/
│   │       └── page.tsx
│   ├── layout.tsx                # Root layout (auth provider, nav)
│   └── api/                      # API routes (proxy to backend if needed)
│       └── auth/
│           └── [...nextauth]/route.ts
├── components/                   # Reusable React components
│   ├── ui/                       # Shadcn/ui components (Button, Input, etc.)
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── select.tsx
│   │   └── dialog.tsx
│   ├── incidents/                # Incident-specific components
│   │   ├── incident-list.tsx     # Client Component (interactive table)
│   │   ├── incident-form.tsx     # Client Component (form)
│   │   ├── incident-card.tsx     # Server Component (display)
│   │   └── sla-timer.tsx         # Client Component (countdown)
│   ├── changes/
│   │   ├── change-calendar.tsx   # Client Component (calendar view)
│   │   └── change-approval-badge.tsx
│   ├── tasks/
│   │   ├── kanban-board.tsx      # Client Component (drag-drop)
│   │   └── task-card.tsx
│   ├── cross-module/             # Trust + UX Foundation components
│   │   ├── single-entry-time-tray.tsx  # Client Component (global worklog)
│   │   ├── freshness-badge.tsx         # Client Component (projection lag)
│   │   ├── link-on-action-button.tsx   # Client Component (create related)
│   │   └── decision-receipt-viewer.tsx # Server Component (policy explanation)
│   └── layout/
│       ├── header.tsx            # Server Component (top nav)
│       ├── sidebar.tsx           # Client Component (collapsible nav)
│       └── notification-center.tsx # Client Component (alerts)
├── lib/                          # Client libraries and utilities
│   ├── api/                      # API client services
│   │   ├── client.ts             # Fetch wrapper with JWT, correlation IDs
│   │   ├── incidents.ts          # Incident API calls
│   │   ├── changes.ts            # Change API calls
│   │   └── tasks.ts              # Task API calls
│   ├── auth/
│   │   └── session.ts            # Session management utilities
│   ├── utils.ts                  # Shared utilities (date formatting, etc.)
│   └── constants.ts              # Constants (API URLs, etc.)
├── types/                        # TypeScript types (generated from backend DTOs)
│   ├── incident.ts
│   ├── change.ts
│   ├── task.ts
│   └── api.ts
├── hooks/                        # Custom React hooks
│   ├── use-incidents.ts          # TanStack Query hook for incidents
│   ├── use-auth.ts               # Auth hook (user session)
│   └── use-freshness.ts          # Projection lag tracking hook
└── stores/                       # Zustand client state stores
    ├── ui-store.ts               # UI state (sidebar open, modal state)
    └── filter-store.ts           # Filter state (incident filters, etc.)
```

### Component Template (Client Component Example)

```typescript
// components/incidents/incident-list.tsx
'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { incidentsApi } from '@/lib/api/incidents';
import { IncidentCard } from './incident-card';
import { FreshnessBadge } from '@/components/cross-module/freshness-badge';
import type { Incident } from '@/types/incident';

interface IncidentListProps {
  initialData?: Incident[];
}

export function IncidentList({ initialData }: IncidentListProps) {
  const [filters, setFilters] = useState({
    status: 'ALL',
    priority: 'ALL',
  });

  const { data, isLoading, error } = useQuery({
    queryKey: ['incidents', filters],
    queryFn: () => incidentsApi.list(filters),
    initialData: initialData,
    refetchInterval: 30000, // Refetch every 30 seconds
  });

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error loading incidents</div>;

  return (
    <div>
      <div className="mb-4">
        <FreshnessBadge projectionLag={data?.meta.projectionLag} />
      </div>

      {/* Filter controls */}
      <div className="mb-4 flex gap-2">
        <select
          value={filters.status}
          onChange={(e) => setFilters({ ...filters, status: e.target.value })}
        >
          <option value="ALL">All Statuses</option>
          <option value="NEW">New</option>
          <option value="IN_PROGRESS">In Progress</option>
        </select>
      </div>

      {/* Incident cards */}
      <div className="grid gap-4">
        {data?.data.map((incident) => (
          <IncidentCard key={incident.id} incident={incident} />
        ))}
      </div>
    </div>
  );
}
```

## 10.2 State Management Architecture

### State Structure

SynergyFlow uses two state management solutions:
1. **TanStack Query (React Query)** - Server state (API data, caching, refetching)
2. **Zustand** - Client state (UI state, filters, preferences)

**TanStack Query State (Server State):**
```typescript
// hooks/use-incidents.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { incidentsApi } from '@/lib/api/incidents';

export function useIncidents(filters?: IncidentFilters) {
  return useQuery({
    queryKey: ['incidents', filters],
    queryFn: () => incidentsApi.list(filters),
    staleTime: 30000, // Data fresh for 30 seconds
    refetchInterval: 30000, // Refetch every 30 seconds
  });
}

export function useIncident(id: string) {
  return useQuery({
    queryKey: ['incidents', id],
    queryFn: () => incidentsApi.get(id),
    staleTime: 15000,
  });
}

export function useCreateIncident() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: incidentsApi.create,
    onSuccess: () => {
      // Invalidate incidents list to refetch
      queryClient.invalidateQueries({ queryKey: ['incidents'] });
    },
  });
}
```

**Zustand State (Client State):**
```typescript
// stores/ui-store.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface UIState {
  sidebarOpen: boolean;
  timeTraySidebarOpen: boolean;
  toggleSidebar: () => void;
  toggleTimeTray: () => void;
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      sidebarOpen: true,
      timeTraySidebarOpen: false,
      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      toggleTimeTray: () => set((state) => ({ timeTraySidebarOpen: !state.timeTraySidebarOpen })),
    }),
    {
      name: 'ui-storage', // localStorage key
    }
  )
);
```

### State Management Patterns

- **Server State (TanStack Query):** All data from backend API (incidents, changes, tasks, users)
- **Client State (Zustand):** UI state only (sidebar open/closed, modal state, filters)
- **No Global Server State:** Never store server data in Zustand (use TanStack Query cache)
- **Optimistic Updates:** Use TanStack Query `onMutate` for optimistic UI updates
- **Cache Invalidation:** Use `queryClient.invalidateQueries()` after mutations
- **Automatic Refetching:** Configure `refetchInterval` for real-time data updates

## 10.3 Routing Architecture

### Route Organization

Next.js 15 App Router with file-based routing:

```
app/
├── (authenticated)/          # Route group - requires authentication
│   ├── layout.tsx            # Authenticated layout (includes header, sidebar)
│   ├── incidents/
│   │   ├── page.tsx          # /incidents
│   │   ├── [id]/
│   │   │   └── page.tsx      # /incidents/[id]
│   │   └── new/
│   │       └── page.tsx      # /incidents/new
│   ├── changes/
│   ├── tasks/
│   └── dashboard/
├── (public)/                 # Route group - public access
│   ├── knowledge/
│   │   └── page.tsx          # /knowledge (SSG)
│   └── login/
│       └── page.tsx          # /login
└── layout.tsx                # Root layout (auth provider)
```

### Protected Route Pattern

Authentication is handled at the **layout level** using Next.js middleware:

```typescript
// middleware.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { getToken } from 'next-auth/jwt';

export async function middleware(request: NextRequest) {
  const token = await getToken({ req: request });
  const isAuthPage = request.nextUrl.pathname.startsWith('/login');
  const isPublicPage = request.nextUrl.pathname.startsWith('/knowledge');

  // Redirect to login if accessing protected route without token
  if (!token && !isAuthPage && !isPublicPage) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // Redirect to dashboard if accessing login page with token
  if (token && isAuthPage) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
};
```

## 10.4 Frontend Services Layer

### API Client Setup

```typescript
// lib/api/client.ts
import { v4 as uuidv4 } from 'uuid';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'https://synergyflow.example.com/api/v1';

interface FetchOptions extends RequestInit {
  correlationId?: string;
}

export async function apiClient<T>(
  endpoint: string,
  options: FetchOptions = {}
): Promise<T> {
  const { correlationId = uuidv4(), ...fetchOptions } = options;

  const headers = new Headers(fetchOptions.headers);
  headers.set('Content-Type', 'application/json');
  headers.set('X-Correlation-ID', correlationId);

  // Add JWT from session (next-auth)
  const session = await getSession();
  if (session?.accessToken) {
    headers.set('Authorization', `Bearer ${session.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...fetchOptions,
    headers,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new ApiError(error.status, error.detail, correlationId);
  }

  return response.json();
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public detail: string,
    public correlationId: string
  ) {
    super(detail);
    this.name = 'ApiError';
  }
}
```

### Service Example

```typescript
// lib/api/incidents.ts
import { apiClient } from './client';
import type { Incident, CreateIncidentRequest, PaginatedResponse } from '@/types';

export const incidentsApi = {
  list: async (filters?: IncidentFilters): Promise<PaginatedResponse<Incident>> => {
    const params = new URLSearchParams();
    if (filters?.status) params.set('status', filters.status);
    if (filters?.priority) params.set('priority', filters.priority);
    if (filters?.page) params.set('page', filters.page.toString());
    if (filters?.limit) params.set('limit', filters.limit.toString());

    return apiClient<PaginatedResponse<Incident>>(
      `/incidents?${params.toString()}`
    );
  },

  get: async (id: string): Promise<{ data: Incident; freshness: FreshnessBadge }> => {
    return apiClient<{ data: Incident; freshness: FreshnessBadge }>(
      `/incidents/${id}`
    );
  },

  create: async (data: CreateIncidentRequest): Promise<Incident> => {
    return apiClient<Incident>('/incidents', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  update: async (id: string, data: Partial<Incident>): Promise<Incident> => {
    return apiClient<Incident>(`/incidents/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  },

  assign: async (id: string, assignedTo: string): Promise<void> => {
    return apiClient<void>(`/incidents/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify({ assignedTo }),
    });
  },

  resolve: async (id: string, resolution: string): Promise<void> => {
    return apiClient<void>(`/incidents/${id}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ resolution }),
    });
  },

  getRelated: async (id: string): Promise<{
    changes: Change[];
    tasks: Task[];
    freshness: FreshnessBadge;
  }> => {
    return apiClient(`/incidents/${id}/related`);
  },
};
```

---
