# Downstream Envelope Compatibility Verification

Date: 2025-10-07  
Participants: SSE (J. Lee), Search (D. Gomez), Backend (monosense)

## Objective
Verify that downstream consumers — the SSE gateway and the Search Indexer — continue to operate when the `domain-events` envelope includes the Story 1.6 metadata additions (`schema_version`, `region`, `attributes`, etc.).

## Test Summary
- **SSE Gateway**: Uses Jackson records annotated with `@JsonIgnoreProperties(ignoreUnknown = true)` to hydrate only the fields it needs (`aggregate_id`, `event_type`, `payload`). Contract test `DownstreamEnvelopeCompatibilityTest#sseConsumerIgnoresAdditionalEnvelopeMetadata` deserializes a Story 1.6 envelope with the new metadata and asserts the payload is intact.
- **Search Indexer**: Maps events to an index update DTO while preserving auxiliary attributes for tracing. Contract test `DownstreamEnvelopeCompatibilityTest#searchIndexerConsumerDeserializesWithExpandedMetadata` validates the mapper ignores unknown metadata yet surfaces the optional `attributes` map for enriched telemetry.

## Findings
- No consumer logic relies on positional array ordering or brittle JSON paths; both use field-name mapping via Jackson.
- Additional metadata fields remain optional. Consumers automatically ignore fields they do not recognize.
- Indexer retains the `attributes` object without failing, allowing us to log delivery metadata for debugging.

## Next Steps
- Incorporate these contract tests into the CI matrix (completed via Gradle test suite).
- Communicate schema stability expectations to external integrators via updated Story 1.6 documentation.

Status: **Confirmed compatible**
