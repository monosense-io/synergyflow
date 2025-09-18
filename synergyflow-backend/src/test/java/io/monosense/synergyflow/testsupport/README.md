Test support utilities live here:

- containers/: reusable Testcontainers setup (PostgreSQL, Redis/Dragonfly, Kafka)
- events/: Spring Modulith event testing helpers
- api/: API client helpers for tests
- time/: deterministic clock utilities

All utilities are implemented and ready for use in tests.

## Usage Examples

### Testcontainers
```java
// In your test class
@ExtendWith(TestcontainersExtension.class)
class MyIntegrationTest {
    // Containers are automatically started
}
```

### Event Testing
```java
@ApplicationModuleTest
class MyModuleTest {
    
    @Test
    void shouldPublishEvent(PublishedEvents events) {
        // Perform action that publishes event
        
        // Verify event was published
        EventTestHelper.assertEventPublished(events, MyEvent.class);
    }
    
    @Test
    void shouldPublishEventWithAssertions(AssertablePublishedEvents events) {
        // Perform action that publishes event
        
        // Verify event was published with fluent assertions
        assertThat(events).contains(MyEvent.class).matching(event -> event.getProperty().equals("expected"));
    }
}
```

### Time Testing
```java
@Test
void shouldUseDeterministicTime() {
    TestClock clock = new TestClock(Instant.parse("2025-01-01T00:00:00Z"));
    DefaultTimeProvider timeProvider = new DefaultTimeProvider(clock);
    
    // Use timeProvider in your code under test
    Instant now = timeProvider.now(); // Always returns the same instant
}
```

