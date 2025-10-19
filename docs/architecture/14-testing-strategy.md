# 14. Testing Strategy

## 14.1 Testing Pyramid

```
         E2E Tests (Playwright)
        /                      \
       /   Integration Tests    \
      /    (Testcontainers)      \
     /                            \
    /  Frontend Unit   Backend Unit \
   /   (Vitest+RTL)    (JUnit 5)     \
  /______________________________________\
```

**Coverage Targets:**
- Backend Unit Tests: ≥80% code coverage (JaCoCo)
- Frontend Unit Tests: ≥70% code coverage (Vitest)
- Integration Tests: All API endpoints + Event consumers
- E2E Tests: 5 critical user journeys (from PRD)

## 14.2 Backend Testing

### Unit Test Example (JUnit 5 + Mockito)

```java
// incident/IncidentServiceTest.java
@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void createIncident_shouldPublishEvent() {
        // Given
        CreateIncidentRequest request = new CreateIncidentRequest(
                "API is down",
                "Production API returning 500 errors",
                Incident.Priority.CRITICAL,
                Incident.Severity.S1
        );

        Incident savedIncident = Incident.builder()
                .id(UUID.randomUUID())
                .title(request.title())
                .priority(request.priority())
                .build();

        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);

        // When
        IncidentResponse response = incidentService.createIncident(request, "user-123");

        // Then
        assertThat(response.id()).isNotNull();
        assertThat(response.priority()).isEqualTo(Incident.Priority.CRITICAL);

        // Verify event published
        verify(eventPublisher).publishEvent(argThat(event ->
                event instanceof IncidentCreatedEvent &&
                ((IncidentCreatedEvent) event).incidentId().equals(savedIncident.getId())
        ));
    }
}
```

### Integration Test (Spring Modulith + Testcontainers)

```java
// incident/IncidentIntegrationTest.java
@SpringBootTest
@Testcontainers
class IncidentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8");

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createIncident_shouldPersistAndPublishEvent() {
        // Given
        CreateIncidentRequest request = new CreateIncidentRequest(...);

        // When
        ResponseEntity<IncidentResponse> response = restTemplate
                .postForEntity("/api/v1/incidents", request, IncidentResponse.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    @ApplicationModuleTest
    void incidentCreated_shouldTriggerSLATimer() {
        // Spring Modulith scenario test - verify event consumption
        // ...
    }
}
```

## 14.3 Frontend Testing

### Component Test (Vitest + React Testing Library)

```typescript
// components/incidents/incident-list.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { IncidentList } from './incident-list';
import { incidentsApi } from '@/lib/api/incidents';

vi.mock('@/lib/api/incidents');

describe('IncidentList', () => {
  it('renders incidents from API', async () => {
    const mockIncidents = [
      { id: '1', title: 'API Down', priority: 'CRITICAL', status: 'NEW' },
      { id: '2', title: 'Slow Response', priority: 'HIGH', status: 'IN_PROGRESS' },
    ];

    vi.mocked(incidentsApi.list).mockResolvedValue({
      data: mockIncidents,
      meta: { page: 1, limit: 50, total: 2, hasNext: false },
    });

    const queryClient = new QueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <IncidentList />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('API Down')).toBeInTheDocument();
      expect(screen.getByText('Slow Response')).toBeInTheDocument();
    });
  });
});
```

## 14.4 E2E Testing (Playwright)

```typescript
// e2e/incident-creation.spec.ts
import { test, expect } from '@playwright/test';

test('create incident end-to-end', async ({ page }) => {
  // Login
  await page.goto('https://synergyflow.example.com/login');
  await page.fill('[name="email"]', 'agent@example.com');
  await page.fill('[name="password"]', 'password123');
  await page.click('button[type="submit"]');

  // Navigate to incidents
  await page.waitForURL('**/dashboard');
  await page.click('a[href="/incidents"]');

  // Create incident
  await page.click('button:has-text("Create Incident")');
  await page.fill('[name="title"]', 'Test Incident');
  await page.fill('[name="description"]', 'Test description');
  await page.selectOption('[name="priority"]', 'HIGH');
  await page.selectOption('[name="severity"]', 'S2');
  await page.click('button:has-text("Submit")');

  // Verify creation
  await page.waitForURL('**/incidents/*');
  await expect(page.locator('h1')).toContainText('Test Incident');
  await expect(page.locator('[data-testid="priority-badge"]')).toContainText('HIGH');
});
```

---
