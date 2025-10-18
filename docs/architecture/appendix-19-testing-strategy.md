# Appendix 19: Testing Strategy - Code Examples and Patterns

**Related Section:** Section 19 - Testing Strategy
**Purpose:** Full code examples for testing patterns referenced in main architecture document

---

## A19.1 Backend Unit Testing Examples

### Domain Service Testing Pattern (JUnit 5 + Mockito)

```java
package io.monosense.synergyflow.incident.domain;

import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentMapper incidentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    @Test
    void createIncident_withValidCommand_shouldSaveAndPublishEvent() {
        // Arrange
        var command = new CreateIncidentCommand(
            "Production API returning 500 errors",
            "S1",
            "High",
            "user-123"
        );

        var incident = Incident.builder()
            .id("INC-1234")
            .title(command.title())
            .severity(command.severity())
            .priority(command.priority())
            .assignedTo(command.assignedTo())
            .status("New")
            .build();

        when(incidentMapper.insert(any(Incident.class))).thenReturn(1);

        // Act
        var result = incidentService.createIncident(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).startsWith("INC-");

        // Verify event published with correct data
        ArgumentCaptor<IncidentCreatedEvent> eventCaptor =
            ArgumentCaptor.forClass(IncidentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        IncidentCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.incidentId()).isEqualTo(result.getId());
        assertThat(publishedEvent.title()).isEqualTo(command.title());
        assertThat(publishedEvent.severity()).isEqualTo(command.severity());
    }

    @Test
    void createIncident_withNullTitle_shouldThrowValidationException() {
        // Arrange
        var command = new CreateIncidentCommand(null, "S1", "High", "user-123");

        // Act & Assert
        assertThatThrownBy(() -> incidentService.createIncident(command))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Title is required");

        verify(incidentMapper, never()).insert(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
```

### Controller Testing Pattern (Spring MockMvc)

```java
package io.monosense.synergyflow.incident.application;

import io.monosense.synergyflow.incident.domain.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentService incidentService;

    @Test
    @WithMockUser(roles = "AGENT")
    void createIncident_withValidRequest_shouldReturn201() throws Exception {
        // Arrange
        var incident = Incident.builder()
            .id("INC-1234")
            .title("Production API down")
            .severity("S1")
            .build();

        when(incidentService.createIncident(any())).thenReturn(incident);

        // Act & Assert
        mockMvc.perform(post("/api/v1/incidents")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "title": "Production API down",
                        "severity": "S1",
                        "priority": "High",
                        "assignedTo": "user-123"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/incidents/INC-1234"))
            .andExpect(jsonPath("$.id", is("INC-1234")))
            .andExpect(jsonPath("$.title", is("Production API down")));
    }
}
```

---

## A19.2 Frontend Unit Testing Examples

### React Component Testing (Vitest + React Testing Library)

```typescript
// TimeEntryTray.test.tsx
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TimeEntryTray } from './TimeEntryTray';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('TimeEntryTray', () => {
  it('should mirror time entry to linked incident and task', async () => {
    // Arrange
    const onClose = vi.fn();
    const mockPost = vi.fn().mockResolvedValue({ data: { success: true } });

    render(<TimeEntryTray isOpen linkedEntities={['INC-1234', 'TASK-890']} onClose={onClose} />, { wrapper });

    // Act
    const durationInput = screen.getByLabelText(/duration/i);
    const descriptionInput = screen.getByLabelText(/description/i);
    const submitButton = screen.getByRole('button', { name: /log time/i });

    fireEvent.change(durationInput, { target: { value: '30m' } });
    fireEvent.change(descriptionInput, { target: { value: 'Analyzed logs' } });
    fireEvent.click(submitButton);

    // Assert
    await waitFor(() => {
      expect(screen.getByText(/Time logged to INC-1234, TASK-890/i)).toBeInTheDocument();
    });

    expect(mockPost).toHaveBeenCalledWith('/api/v1/time-entries', {
      duration: '30m',
      description: 'Analyzed logs',
      linkedEntities: ['INC-1234', 'TASK-890'],
    });
  });

  it('should show validation error for missing duration', async () => {
    render(<TimeEntryTray isOpen linkedEntities={['INC-1234']} onClose={vi.fn()} />, { wrapper });

    const submitButton = screen.getByRole('button', { name: /log time/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/Duration is required/i)).toBeInTheDocument();
    });
  });

  it('should display freshness badge with projection lag', async () => {
    render(<TimeEntryTray isOpen linkedEntities={['INC-1234']} onClose={vi.fn()} />, { wrapper });

    const badge = screen.getByTestId('freshness-badge');
    expect(badge).toHaveClass('bg-green'); // <100ms = green
    expect(badge).toHaveTextContent(/2.3 seconds ago/i);
  });
});
```

### Custom Hook Testing (Vitest)

```typescript
// useFreshnessBadge.test.ts
import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { useFreshnessBadge } from './useFreshnessBadge';

describe('useFreshnessBadge', () => {
  it('should return green badge for lag <100ms', () => {
    const { result } = renderHook(() => useFreshnessBadge(50));

    expect(result.current.color).toBe('green');
    expect(result.current.label).toBe('50ms ago');
    expect(result.current.threshold).toBe('< 100ms');
  });

  it('should return yellow badge for lag 100-500ms', () => {
    const { result } = renderHook(() => useFreshnessBadge(250));

    expect(result.current.color).toBe('yellow');
    expect(result.current.label).toBe('250ms ago');
    expect(result.current.threshold).toBe('100-500ms');
  });

  it('should return red badge for lag >500ms', () => {
    const { result } = renderHook(() => useFreshnessBadge(800));

    expect(result.current.color).toBe('red');
    expect(result.current.label).toBe('800ms ago');
    expect(result.current.threshold).toBe('> 500ms');
  });
});
```

---

## A19.3 Integration Testing Examples

### Spring Modulith Event Publication Testing

```java
package io.monosense.synergyflow.incident;

import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.application.CreateIncidentCommand;
import io.monosense.synergyflow.incident.domain.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ApplicationModuleTest
class IncidentModuleIntegrationTest {

    @Autowired
    IncidentService incidentService;

    @Test
    void createIncident_shouldPublishEventToEventPublicationTable(Scenario scenario) throws Exception {
        // Arrange
        var command = new CreateIncidentCommand(
            "Production API returning 500 errors",
            "S1",
            "High",
            "user-123"
        );

        // Act
        var result = scenario.stimulate(() -> incidentService.createIncident(command))
            .andWaitForEventOfType(IncidentCreatedEvent.class)
            .matching(event -> event.incidentId().equals(result.getId()))
            .toArrive();

        // Assert
        result.andExpect(event -> {
            assertThat(event.incidentId()).startsWith("INC-");
            assertThat(event.title()).isEqualTo(command.title());
            assertThat(event.severity()).isEqualTo("S1");
            assertThat(event.priority()).isEqualTo("High");
            assertThat(event.correlationId()).isNotNull();
        });

        // Verify event persisted in event_publication table
        assertThat(eventPublicationRepository.findByEventType(IncidentCreatedEvent.class.getName()))
            .isNotEmpty()
            .anyMatch(ep -> ep.getSerializedEvent().contains(result.getId()));
    }
}
```

### Spring Modulith Event Consumer Testing

```java
package io.monosense.synergyflow.task.internal;

import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import io.monosense.synergyflow.task.domain.TaskProjectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ApplicationModuleTest
class IncidentEventConsumerTest {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    TaskProjectionRepository taskProjectionRepo;

    @Test
    void onIncidentCreated_shouldCreateProjectionInTaskModule() {
        // Arrange
        var event = new IncidentCreatedEvent(
            "INC-1234",
            "Production API down",
            "S1",
            "High",
            Instant.now(),
            "correlation-123",
            "causation-456"
        );

        // Act
        eventPublisher.publishEvent(event);

        // Assert - wait for eventual consistency
        await().atMost(Duration.ofMillis(200))
            .until(() -> taskProjectionRepo.findByIncidentId("INC-1234").isPresent());

        var projection = taskProjectionRepo.findByIncidentId("INC-1234").get();
        assertThat(projection.getIncidentTitle()).isEqualTo("Production API down");
        assertThat(projection.getSeverity()).isEqualTo("S1");
        assertThat(projection.getLastUpdated()).isNotNull();
    }

    @Test
    void onIncidentCreated_withDuplicateEvent_shouldBeIdempotent() {
        // Arrange
        var event = new IncidentCreatedEvent(...);

        // Act - publish same event twice
        eventPublisher.publishEvent(event);
        eventPublisher.publishEvent(event);

        // Assert - only one projection created
        await().atMost(Duration.ofMillis(200))
            .until(() -> taskProjectionRepo.findByIncidentId("INC-1234").isPresent());

        long count = taskProjectionRepo.countByIncidentId("INC-1234");
        assertThat(count).isEqualTo(1); // Idempotent
    }
}
```

### Database Integration Testing with Testcontainers

```java
package io.monosense.synergyflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class DatabaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.8")
        .withDatabaseName("synergyflow_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword());
    }

    @Test
    void flywayMigrations_shouldRunSuccessfully() {
        // Flyway migrations auto-run on test startup
        // Test passes if context loads without migration errors
    }
}
```

---

## A19.4 Contract Testing Examples

### Spring Cloud Contract - Producer Side

```groovy
// contracts/incident/incident-created-event.groovy
package contracts.incident

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "IncidentCreatedEvent Contract"
    description "Contract for IncidentCreatedEvent published by Incident module"

    input {
        triggeredBy("createIncident()")
    }

    outputMessage {
        sentTo("incident.created")

        body([
            incidentId: value(consumer(regex('[A-Z]{3}-[0-9]{4}')), producer('INC-1234')),
            title: value(consumer(regex('.{5,200}')), producer('Production API returning 500 errors')),
            severity: value(consumer(anyOf('S1', 'S2', 'S3', 'S4')), producer('S1')),
            priority: value(consumer(anyOf('Low', 'Medium', 'High', 'Critical')), producer('High')),
            assignedTo: value(consumer(regex('user-[0-9]+')), producer('user-123')),
            createdAt: value(consumer(regex('[0-9]{4}-[0-9]{2}-[0-9]{2}T.*')), producer('2025-10-18T10:00:00Z')),
            correlationId: value(consumer(regex('[a-f0-9\\-]{36}')), producer('550e8400-e29b-41d4-a716-446655440000')),
            causationId: value(consumer(optional(regex('[a-f0-9\\-]{36}'))), producer(null))
        ])

        headers {
            header("eventType", "IncidentCreatedEvent")
            header("contentType", "application/json")
        }
    }
}
```

### Contract Test - Generated Producer Test

```java
// Generated by Spring Cloud Contract
public class IncidentCreatedEventContractTest extends ContractVerifierBase {

    @Test
    public void validate_incidentCreatedEvent() throws Exception {
        // Given
        createIncident();

        // When
        ContractVerifierMessage response = contractVerifierMessaging().receive("incident.created");

        // Then
        assertThat(response).isNotNull();
        assertThat(JsonPath.read(response.getPayloadAsString(), "$.incidentId")).matches("[A-Z]{3}-[0-9]{4}");
        assertThat(JsonPath.read(response.getPayloadAsString(), "$.severity")).isIn("S1", "S2", "S3", "S4");
        assertThat(response.getHeader("eventType")).isEqualTo("IncidentCreatedEvent");
    }
}
```

### Contract Test - Consumer Side Stub

```java
package io.monosense.synergyflow.task.internal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

@SpringBootTest
@AutoConfigureStubRunner(
    ids = "io.monosense:incident-module:+:stubs",
    stubsMode = StubRunnerProperties.StubsMode.REMOTE
)
class IncidentEventConsumerContractTest {

    @Autowired
    StubTrigger stubTrigger;

    @Autowired
    TaskProjectionRepository taskProjectionRepo;

    @Test
    void shouldConsumeIncidentCreatedEventMatchingContract() {
        // Trigger stub to publish contracted event
        stubTrigger.trigger("incident.created");

        // Verify consumer handles event correctly
        await().until(() -> taskProjectionRepo.findByIncidentId("INC-1234").isPresent());
    }
}
```

---

## A19.5 End-to-End Testing Examples

### Playwright Test - Single-Entry Time Tray

```typescript
// e2e/time-tray.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Single-Entry Time Tray', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="email"]', 'agent@example.com');
    await page.fill('[name="password"]', 'password123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  });

  test('should mirror time entry to linked incident and task', async ({ page }) => {
    // Navigate to incident detail page
    await page.goto('/incidents/INC-1234');
    await expect(page.locator('h1')).toContainText('INC-1234');

    // Open Time Tray
    await page.click('[data-testid="time-tray-toggle"]');
    await expect(page.locator('[data-testid="time-tray"]')).toBeVisible();

    // Fill time entry form
    await page.fill('[name="duration"]', '30m');
    await page.fill('[name="description"]', 'Analyzed production logs for root cause');

    // Auto-detect linked entities (incident + task from context)
    const linkedEntities = page.locator('[data-testid="linked-entities"]');
    await expect(linked Entities).toContainText('INC-1234');
    await expect(linkedEntities).toContainText('TASK-890');

    // Submit time entry
    await page.click('[data-testid="submit-time-entry"]');

    // Verify success toast
    await expect(page.locator('.toast.success')).toBeVisible();
    await expect(page.locator('.toast.success')).toContainText('Time logged to INC-1234, TASK-890');

    // Verify mirroring to incident worklog (eventual consistency)
    await expect(page.locator('[data-testid="incident-worklog"]')).toContainText('30m');
    await expect(page.locator('[data-testid="incident-worklog"]')).toContainText('Analyzed production logs');

    // Verify freshness badge shows recent update
    const freshnessBadge = page.locator('[data-testid="freshness-badge"]');
    await expect(freshnessBadge).toHaveClass(/bg-green/); // <100ms = green
    await expect(freshnessBadge).toContainText(/\d+\.\d+ seconds ago/);

    // Navigate to linked task and verify mirroring
    await page.click('[data-testid="link-TASK-890"]');
    await expect(page).toHaveURL(/\/tasks\/TASK-890/);
    await expect(page.locator('[data-testid="task-worklog"]')).toContainText('30m');
  });

  test('should show validation errors for invalid input', async ({ page }) => {
    await page.goto('/incidents/INC-1234');
    await page.click('[data-testid="time-tray-toggle"]');

    // Submit without duration
    await page.click('[data-testid="submit-time-entry"]');

    // Verify validation error
    await expect(page.locator('[data-testid="duration-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="duration-error"]')).toContainText('Duration is required');

    // Verify no API call made
    const requests = page.context().waitForEvent('request', req =>
      req.url().includes('/api/v1/time-entries')
    );
    expect(requests).toBeNull();
  });

  test('should handle network errors gracefully', async ({ page, context }) => {
    // Mock network failure
    await context.route('**/api/v1/time-entries', route => route.abort());

    await page.goto('/incidents/INC-1234');
    await page.click('[data-testid="time-tray-toggle"]');
    await page.fill('[name="duration"]', '30m');
    await page.click('[data-testid="submit-time-entry"]');

    // Verify error toast
    await expect(page.locator('.toast.error')).toBeVisible();
    await expect(page.locator('.toast.error')).toContainText('Failed to log time. Please try again.');

    // Verify retry button shown
    await expect(page.locator('[data-testid="retry-button"]')).toBeVisible();
  });
});
```

### Playwright Test - Link-on-Action

```typescript
// e2e/link-on-action.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Link-on-Action Cross-Module Workflows', () => {
  test('should create related change from incident with auto-populated context', async ({ page }) => {
    // Login and navigate to incident
    await page.goto('/login');
    // ... login steps ...
    await page.goto('/incidents/INC-5678');

    // Click "Create related change" button
    await page.click('[data-testid="link-action-create-change"]');

    // Verify change creation modal opened
    await expect(page.locator('[data-testid="create-change-modal"]')).toBeVisible();

    // Verify auto-populated fields from incident context
    await expect(page.locator('[name="change-title"]')).toHaveValue('Rollback deployment related to INC-5678');
    await expect(page.locator('[name="risk"]')).toHaveValue('High'); // Auto-detected from incident severity S1
    await expect(page.locator('[name="impacted-services"]')).toContainText('API-Service'); // From incident context
    await expect(page.locator('[data-testid="linked-incident"]')).toContainText('INC-5678');

    // Fill remaining required fields
    await page.fill('[name="change-description"]', 'Rollback API deployment to previous stable version');
    await page.click('[name="scheduled-date"]');
    await page.click('[data-testid="date-tomorrow"]');

    // Submit change creation
    await page.click('[data-testid="submit-create-change"]');

    // Verify success toast
    await expect(page.locator('.toast.success')).toContainText('Change CHG-9012 created and linked to INC-5678');

    // Verify bidirectional link established
    await expect(page.locator('[data-testid="related-changes"]')).toContainText('CHG-9012');

    // Navigate to change and verify reverse link
    await page.click('[data-testid="link-CHG-9012"]');
    await expect(page).toHaveURL(/\/changes\/CHG-9012/);
    await expect(page.locator('[data-testid="related-incidents"]')).toContainText('INC-5678');

    // Verify freshness badge shows recent link
    await expect(page.locator('[data-testid="freshness-badge"]')).toHaveClass(/bg-green/);
  });
});
```

---

## A19.6 Performance Testing Examples

### K6 Load Test - Normal Load (250 concurrent users)

```javascript
// k6/normal-load.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const apiLatency = new Trend('api_latency');
const eventLag = new Trend('event_processing_lag');

export let options = {
  stages: [
    { duration: '2m', target: 50 },    // Ramp-up to 50 users
    { duration: '5m', target: 250 },   // Steady state at 250 users (NFR target)
    { duration: '2m', target: 0 },     // Ramp-down to 0
  ],
  thresholds: {
    'http_req_duration{p95}': ['<200'],     // NFR-1: p95 <200ms
    'http_req_duration{p99}': ['<500'],     // NFR-1: p99 <500ms
    'http_req_failed': ['<0.01'],            // <1% error rate
    'event_processing_lag{p95}': ['<200'],   // Event lag p95 <200ms
  },
};

const BASE_URL = __ENV.API_URL || 'https://api.synergyflow.io';

export function setup() {
  // Authenticate and get JWT token
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: 'loadtest@example.com',
    password: 'loadtest123',
  }), { headers: { 'Content-Type': 'application/json' } });

  return { token: loginRes.json('token') };
}

export default function(data) {
  const headers = {
    'Authorization': `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };

  // Scenario 1: Create incident (20% of requests)
  if (Math.random() < 0.2) {
    const createRes = http.post(`${BASE_URL}/api/v1/incidents`, JSON.stringify({
      title: `Load test incident ${Date.now()}`,
      severity: 'S3',
      priority: 'Medium',
      description: 'Performance testing incident',
    }), { headers });

    check(createRes, {
      'incident created': (r) => r.status === 201,
      'response time <200ms': (r) => r.timings.duration < 200,
    });

    errorRate.add(createRes.status !== 201);
    apiLatency.add(createRes.timings.duration);
  }

  // Scenario 2: List incidents (50% of requests)
  if (Math.random() < 0.5) {
    const listRes = http.get(`${BASE_URL}/api/v1/incidents?page=0&size=20`, { headers });

    check(listRes, {
      'incidents listed': (r) => r.status === 200,
      'response time <200ms': (r) => r.timings.duration < 200,
    });

    errorRate.add(listRes.status !== 200);
    apiLatency.add(listRes.timings.duration);
  }

  // Scenario 3: Update incident (30% of requests)
  if (Math.random() < 0.3) {
    const updateRes = http.patch(`${BASE_URL}/api/v1/incidents/INC-1234`, JSON.stringify({
      status: 'In Progress',
    }), { headers });

    check(updateRes, {
      'incident updated': (r) => r.status === 200,
      'response time <200ms': (r) => r.timings.duration < 200,
      'freshness badge present': (r) => r.json('freshnessLag') !== undefined,
    });

    errorRate.add(updateRes.status !== 200);
    apiLatency.add(updateRes.timings.duration);

    // Measure event processing lag (freshness badge)
    if (updateRes.status === 200) {
      eventLag.add(updateRes.json('freshnessLag'));
    }
  }

  sleep(1); // 1 second think time
}

export function teardown(data) {
  // Cleanup test data
  console.log('Load test completed');
}
```

### K6 Stress Test - 1,000 Concurrent Users

```javascript
// k6/stress-test.js
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 },    // Warm-up
    { duration: '5m', target: 500 },    // Ramp to 500
    { duration: '5m', target: 1000 },   // Stress test (4x MVP target)
    { duration: '5m', target: 1000 },   // Sustain stress
    { duration: '5m', target: 0 },      // Ramp down
  ],
  thresholds: {
    'http_req_duration{p95}': ['<500'],      // Allow higher latency under stress
    'http_req_failed': ['<0.05'],             // Allow 5% error rate under stress
  },
};

export default function() {
  // Same test scenarios as normal-load.js
  // Focus on identifying bottlenecks: database connections, OPA sidecar, cache
}
```

---

## A19.7 Security Testing Examples

### OWASP Dependency Check Configuration

```xml
<!-- pom.xml (Maven) or build.gradle (Gradle) -->
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>9.0.0</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS> <!-- Fail on HIGH severity CVEs -->
    <suppressionFile>owasp-suppressions.xml</suppressionFile>
  </configuration>
</plugin>
```

### Trivy Container Scanning (CI/CD)

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on: [push, pull_request]

jobs:
  trivy-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: docker build -t synergyflow-backend:${{ github.sha }} .

      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: synergyflow-backend:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'

      - name: Upload Trivy results to GitHub Security
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: 'trivy-results.sarif'
```

---

## A19.8 CI/CD Test Pipeline Examples

### GitHub Actions - Complete Test Pipeline

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend-unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run unit tests with coverage
        run: ./gradlew test jacocoTestReport

      - name: Upload coverage to SonarQube
        run: ./gradlew sonarqube
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

      - name: Publish test results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: build/test-results/**/*.xml

  frontend-unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm ci

      - name: Run Vitest tests with coverage
        run: npm run test:coverage

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./coverage/coverage-final.json

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16.8
        env:
          POSTGRES_DB: synergyflow_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      dragonfly:
        image: docker.dragonflydb.io/dragonflydb/dragonfly:v1.17.0
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'

      - name: Run integration tests
        run: ./gradlew integrationTest
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/synergyflow_test
          SPRING_REDIS_HOST: localhost

  contract-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Run contract tests
        run: ./gradlew contractTest

      - name: Publish contract stubs
        run: ./gradlew publishStubsToScm
        if: github.ref == 'refs/heads/main'

  e2e-tests:
    runs-on: ubuntu-latest
    needs: [backend-unit-tests, frontend-unit-tests, integration-tests]
    steps:
      - uses: actions/checkout@v4

      - name: Deploy to staging
        run: |
          # Deploy backend and frontend to staging environment
          kubectl apply -f k8s/staging/

      - name: Run Playwright E2E tests
        run: npx playwright test

      - name: Upload Playwright report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: playwright-report/

  quality-gates:
    runs-on: ubuntu-latest
    needs: [backend-unit-tests, frontend-unit-tests, integration-tests, contract-tests]
    steps:
      - name: Check SonarQube quality gate
        run: |
          # Wait for SonarQube analysis to complete
          # Fail if quality gate fails (coverage <80%, critical bugs found)

      - name: Check contract validation
        run: |
          # Fail if contract tests found breaking changes

      - name: Performance regression check
        run: |
          # Compare API latency p95 against baseline
          # Fail if regression >10%
```

---

## A19.9 Test Data Management Examples

### Test Data Builder Pattern

```java
package io.monosense.synergyflow.testutil;

import io.monosense.synergyflow.incident.domain.Incident;
import lombok.Builder;

@Builder
public class IncidentTestDataBuilder {

    @Builder.Default
    private String id = "INC-TEST-" + System.currentTimeMillis();

    @Builder.Default
    private String title = "Test incident";

    @Builder.Default
    private String severity = "S3";

    @Builder.Default
    private String priority = "Medium";

    @Builder.Default
    private String status = "New";

    private String assignedTo;
    private String description;

    public Incident build() {
        return Incident.builder()
            .id(id)
            .title(title)
            .severity(severity)
            .priority(priority)
            .status(status)
            .assignedTo(assignedTo)
            .description(description)
            .build();
    }

    // Convenience factory methods
    public static Incident criticalIncident() {
        return IncidentTestDataBuilder.builder()
            .severity("S1")
            .priority("Critical")
            .build();
    }

    public static Incident assignedIncident(String userId) {
        return IncidentTestDataBuilder.builder()
            .assignedTo(userId)
            .status("Assigned")
            .build();
    }
}

// Usage in tests:
@Test
void testCriticalIncident() {
    Incident incident = IncidentTestDataBuilder.criticalIncident();
    // Test with critical incident
}
```

### Flyway Test Data Migration

```sql
-- src/test/resources/db/migration/V999__test_data.sql
-- Test data for integration tests (V999 prefix ensures it runs last, only in test profile)

-- Test users
INSERT INTO users (id, email, name, role, created_at) VALUES
  ('user-agent-1', 'agent1@test.com', 'Test Agent 1', 'AGENT', NOW()),
  ('user-manager-1', 'manager1@test.com', 'Test Manager 1', 'MANAGER', NOW());

-- Test incidents
INSERT INTO incidents (id, title, severity, priority, status, assigned_to, created_at) VALUES
  ('INC-TEST-001', 'Test incident 1', 'S3', 'Medium', 'New', NULL, NOW()),
  ('INC-TEST-002', 'Test incident 2', 'S1', 'Critical', 'Assigned', 'user-agent-1', NOW());

-- Test tasks
INSERT INTO tasks (id, title, status, assigned_to, project_id, created_at) VALUES
  ('TASK-TEST-001', 'Test task 1', 'To Do', 'user-agent-1', 'PROJ-001', NOW());

-- Test relationships
INSERT INTO entity_relationships (source_type, source_id, target_type, target_id, relationship_type) VALUES
  ('INCIDENT', 'INC-TEST-002', 'TASK', 'TASK-TEST-001', 'RELATED_TO');
```

---

_End of Testing Strategy Appendix_
