package io.monosense.synergyflow.sse.api;

import io.monosense.synergyflow.sse.internal.SseCatchupService;
import io.monosense.synergyflow.sse.internal.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

/**
 * REST controller for Server-Sent Events (SSE) streaming.
 * <p>
 * Provides real-time event streaming to web clients via the SSE protocol (HTTP/1.1 or HTTP/2).
 * Clients connect to {@code GET /api/sse/events} with JWT authentication and receive domain
 * events as they occur. Supports automatic reconnection via the {@code Last-Event-ID} header,
 * which triggers catchup of missed events from Redis Stream.
 * </p>
 * <p>
 * <strong>SSE Protocol Details:</strong>
 * </p>
 * <ul>
 *     <li>Content-Type: {@code text/event-stream}</li>
 *     <li>Event format: {@code id: <event-id>\ndata: <json-payload>\n\n}</li>
 *     <li>Browser {@code EventSource} API automatically reconnects on disconnect</li>
 *     <li>Infinite server-side timeout ({@link Long#MAX_VALUE}); clients handle reconnection</li>
 *     <li>Stateless - no server session required, JWT auth on each connection</li>
 * </ul>
 * <p>
 * <strong>Reconnection Behavior:</strong>
 * </p>
 * <p>
 * When a client reconnects after disconnection, it sends the {@code Last-Event-ID} header
 * containing the ID of the last successfully received event. The controller reads all missed
 * events from Redis Stream since that ID and sends them as a catchup batch before resuming
 * live event streaming. Catchup completes in &lt;1s p95 for ≤100 missed events.
 * </p>
 * <p>
 * <strong>Authentication Requirements:</strong>
 * </p>
 * <p>
 * Requires valid JWT token via Spring Security OAuth2 Resource Server. User ID is extracted
 * from {@link Principal#getName()} for connection tracking and event filtering.
 * </p>
 * <p>
 * <strong>Frontend Integration Example:</strong>
 * </p>
 * <pre>{@code
 * const eventSource = new EventSource('/api/sse/events', { withCredentials: true });
 * eventSource.onmessage = (event) => {
 *     const envelope = JSON.parse(event.data);
 *     // Handle event based on envelope.event_type
 *     // Implement idempotent reducer: drop if version <= currentState.version
 * };
 * }</pre>
 *
 * @author monosense
 * @since 1.7
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events">MDN: Server-Sent Events</a>
 * @see <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">WHATWG: Server-Sent Events Spec</a>
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
@Profile("!worker")
public class SseController {

    private final SseConnectionManager connectionManager;
    private final SseCatchupService catchupService;

    /**
     * Establishes an SSE connection for the authenticated user.
     * <p>
     * Creates a new {@link SseEmitter} with infinite timeout, registers it in the connection
     * manager, and optionally performs catchup if {@code Last-Event-ID} header is present.
     * </p>
     * <p>
     * Connection lifecycle is managed through emitter callbacks:
     * </p>
     * <ul>
     *     <li>{@code onCompletion}: Normal connection close, unregister cleanly</li>
     *     <li>{@code onError}: Connection error (network failure, client closed), unregister and log error</li>
     *     <li>{@code onTimeout}: Timeout (should never occur with {@code Long.MAX_VALUE}), unregister</li>
     * </ul>
     *
     * @param principal the authenticated user principal (provided by Spring Security)
     * @param lastEventId optional last event ID for reconnection (Redis Stream ID format)
     * @return the SSE emitter instance for streaming events
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            Principal principal,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        String userId = principal.getName();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        log.info("SSE connection request: user_id={}, last_event_id={}", userId, lastEventId);

        // Register connection
        String connectionId = connectionManager.register(userId, emitter, lastEventId);

        // Set up lifecycle callbacks
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: user_id={}, connection_id={}", userId, connectionId);
            connectionManager.unregister(userId, emitter);
        });

        emitter.onError((error) -> {
            log.warn("SSE connection error: user_id={}, connection_id={}, error={}",
                    userId, connectionId, error.getMessage());
            connectionManager.unregister(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout: user_id={}, connection_id={} (unexpected with Long.MAX_VALUE)",
                    userId, connectionId);
            connectionManager.unregister(userId, emitter);
        });

        // Perform catchup if reconnection
        if (lastEventId != null && !lastEventId.isBlank()) {
            log.info("Starting SSE catchup: user_id={}, connection_id={}, last_event_id={}",
                    userId, connectionId, lastEventId);
            catchupService.sendMissedEvents(userId, emitter, lastEventId);
        }

        return emitter;
    }
}
