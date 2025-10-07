/**
 * Server-Sent Events Module - Real-time Gateway
 * <p>
 * This module provides real-time event streaming to clients via Server-Sent Events (SSE).
 * Consumes events from the eventing module and pushes to connected clients.
 * </p>
 *
 * @see io.monosense.synergyflow.sse.api
 */
@ApplicationModule(
    displayName = "Server-Sent Events Module",
    allowedDependencies = {"eventing"}
)
package io.monosense.synergyflow.sse;

import org.springframework.modulith.ApplicationModule;
