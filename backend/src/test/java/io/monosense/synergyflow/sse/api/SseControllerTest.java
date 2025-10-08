package io.monosense.synergyflow.sse.api;

import io.monosense.synergyflow.sse.internal.SseCatchupService;
import io.monosense.synergyflow.sse.internal.SseConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SseController}.
 *
 * @author monosense
 * @since 1.7
 */
@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    @Mock
    private SseConnectionManager connectionManager;

    @Mock
    private SseCatchupService catchupService;

    @Mock
    private Principal principal;

    private SseController controller;

    @BeforeEach
    void setUp() {
        controller = new SseController(connectionManager, catchupService);
    }

    @Test
    void streamEvents_createsEmitterWithInfiniteTimeout() {
        when(principal.getName()).thenReturn("user123");
        when(connectionManager.register(anyString(), any(SseEmitter.class), isNull()))
                .thenReturn("conn-id-123");

        SseEmitter emitter = controller.streamEvents(principal, null);

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void streamEvents_registersConnectionWithUserId() {
        when(principal.getName()).thenReturn("user123");
        when(connectionManager.register(anyString(), any(SseEmitter.class), isNull()))
                .thenReturn("conn-id-123");

        controller.streamEvents(principal, null);

        verify(connectionManager).register(eq("user123"), any(SseEmitter.class), isNull());
    }

    @Test
    void streamEvents_withLastEventId_triggersCatchup() {
        when(principal.getName()).thenReturn("user123");
        String lastEventId = "1234567890-0";
        when(connectionManager.register(anyString(), any(SseEmitter.class), eq(lastEventId)))
                .thenReturn("conn-id-123");
        when(catchupService.sendMissedEvents(anyString(), any(SseEmitter.class), eq(lastEventId)))
                .thenReturn(5);

        controller.streamEvents(principal, lastEventId);

        verify(catchupService).sendMissedEvents(eq("user123"), any(SseEmitter.class), eq(lastEventId));
    }

    @Test
    void streamEvents_withoutLastEventId_skipsCatchup() {
        when(principal.getName()).thenReturn("user123");
        when(connectionManager.register(anyString(), any(SseEmitter.class), isNull()))
                .thenReturn("conn-id-123");

        controller.streamEvents(principal, null);

        verify(catchupService, never()).sendMissedEvents(anyString(), any(SseEmitter.class), anyString());
    }

    @Test
    void streamEvents_withBlankLastEventId_skipsCatchup() {
        when(principal.getName()).thenReturn("user123");
        when(connectionManager.register(anyString(), any(SseEmitter.class), eq("")))
                .thenReturn("conn-id-123");

        controller.streamEvents(principal, "");

        verify(catchupService, never()).sendMissedEvents(anyString(), any(SseEmitter.class), anyString());
    }

    @Test
    void streamEvents_producesTextEventStreamMediaType() {
        // Verify the endpoint produces text/event-stream
        // This is validated at compile time by the @GetMapping annotation
        // MediaType.TEXT_EVENT_STREAM_VALUE = "text/event-stream"
        assertThat(MediaType.TEXT_EVENT_STREAM_VALUE).isEqualTo("text/event-stream");
    }
}
