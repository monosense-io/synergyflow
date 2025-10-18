package io.monosense.synergyflow.time;

import io.monosense.synergyflow.time.api.TimeEntryController;
import io.monosense.synergyflow.time.api.TimeEntryCreatedEvent;
import io.monosense.synergyflow.time.application.TimeEntryService;
import io.monosense.synergyflow.time.domain.TimeEntry;
import io.monosense.synergyflow.time.infrastructure.TimeEntryRepository;
import io.monosense.synergyflow.time.observability.TimeEntryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TimeEntryMetrics metrics;

    private TimeEntryService timeEntryService;

    @BeforeEach
    void setUp() {
        timeEntryService = new TimeEntryService(timeEntryRepository, eventPublisher, metrics);
    }

    @Test
    void createTimeEntry_shouldSaveAndPublishEvent() {
        // Given
        String userId = "user123";
        Duration duration = Duration.ofMinutes(30);
        String description = "Test work";
        Instant occurredAt = Instant.now();
        String correlationId = "corr-123";

        List<TimeEntryController.TargetEntityDto> targets = Arrays.asList(
                new TimeEntryController.TargetEntityDto(
                        TimeEntryCreatedEvent.TargetEntity.EntityType.INCIDENT,
                        "incident-1",
                        "Test Incident"
                )
        );

        TimeEntry savedEntry = TimeEntry.create(userId, duration, description, occurredAt);
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(savedEntry);

        // When
        TimeEntry result = timeEntryService.createTimeEntry(
                userId, duration, description, occurredAt, targets, correlationId
        );

        // Then
        assertThat(result).isNotNull();
        verify(timeEntryRepository).save(any(TimeEntry.class));

        ArgumentCaptor<TimeEntryCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TimeEntryCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TimeEntryCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.userId()).isEqualTo(userId);
        assertThat(publishedEvent.duration()).isEqualTo(duration);
        assertThat(publishedEvent.description()).isEqualTo(description);
        assertThat(publishedEvent.correlationId()).isEqualTo(correlationId);
        assertThat(publishedEvent.targetEntities()).hasSize(1);
        assertThat(publishedEvent.targetEntities().get(0).type()).isEqualTo(
                TimeEntryCreatedEvent.TargetEntity.EntityType.INCIDENT
        );
    }

    @Test
    void createBulkTimeEntries_shouldCreateMultipleEntries() {
        // Given
        String userId = "user123";
        String correlationId = "corr-123";

        List<TimeEntryController.CreateTimeEntryRequest> requests = Arrays.asList(
                new TimeEntryController.CreateTimeEntryRequest(
                        30, "Work 1", Instant.now(), List.of()
                ),
                new TimeEntryController.CreateTimeEntryRequest(
                        60, "Work 2", Instant.now(), List.of()
                )
        );

        TimeEntry savedEntry = TimeEntry.create(userId, Duration.ofMinutes(30), "Work 1", Instant.now());
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(savedEntry);

        // When
        List<String> result = timeEntryService.createBulkTimeEntries(userId, requests, correlationId);

        // Then
        assertThat(result).hasSize(2);
        verify(timeEntryRepository, times(2)).save(any(TimeEntry.class));
        verify(eventPublisher, times(2)).publishEvent(any(TimeEntryCreatedEvent.class));
    }

    @Test
    void getTimeEntry_shouldReturnEntryWhenFound() {
        // Given
        TimeEntry entry = TimeEntry.create("user123", Duration.ofMinutes(30), "Test", Instant.now());
        String entryId = entry.id();
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        // When
        Optional<TimeEntry> result = timeEntryService.getTimeEntry(entryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(entryId);
    }

    @Test
    void getTimeEntry_shouldReturnEmptyWhenNotFound() {
        // Given
        String entryId = "nonexistent";
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        // When
        Optional<TimeEntry> result = timeEntryService.getTimeEntry(entryId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getTimeEntriesByUser_shouldReturnUserEntries() {
        // Given
        String userId = "user123";
        List<TimeEntry> entries = Arrays.asList(
                TimeEntry.create(userId, Duration.ofMinutes(30), "Work 1", Instant.now()),
                TimeEntry.create(userId, Duration.ofMinutes(60), "Work 2", Instant.now())
        );
        when(timeEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(entries);

        // When
        List<TimeEntry> result = timeEntryService.getTimeEntriesByUser(userId);

        // Then
        assertThat(result).hasSize(2);
        verify(timeEntryRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }
}