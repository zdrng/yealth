# Health Connect data source

The SDK boundary has explicit typed readers for all 40 stable ordinary record types in
connect-client 1.1.0. `RecordAdapter` handles exact query/cursor paging; `RecordMappers`,
`StableRecordMappers` and `SessionMappers` retain raw values, canonical units, explicit nullable
fields, samples, nested sessions and metadata. No reflection is used in production mappings.

The domain repository contract exposes raw pages and supported aggregates. Provider errors are
redacted, cancellation propagates, and no health records are persisted or logged. SDK activities,
permissions and the per-session exercise-route consent contract are adapted here to neutral
results and wired by the composition root. Route consent requests no GPS/location permission.

Mindfulness remains the sole reviewed experimental deferral; no permission or reader is enabled.
The JVM SDK inventory/schema tests and shared test-only fixtures must be reconciled on upgrades.
