# Domain and service boundary

This package stays pure Kotlin. `model/` will contain neutral health display models,
`repository/` the interfaces consumed by the application, and `service/` the orchestration
for permissions, paging, availability and queries. It must not import Android, Compose,
Health Connect implementations, `data`, `ui`, or `di`.

