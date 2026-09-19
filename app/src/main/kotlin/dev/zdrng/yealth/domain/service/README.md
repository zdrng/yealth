# Domain services

`HealthBrowserService` coordinates every record read and aggregate through the domain
repository contract. It preflights provider availability, feature support, implemented-reader
status, the exact type's permission and optional full-history access before calling the reader.
Category permission requests include only implemented types supported by the current provider.

`metricSummary` exposes provider sleep/exercise duration totals, distance totals and heart-rate
averages. `stepsSummary` exposes the provider's deduplicated total. Neither sums raw pages.
`dailySteps` accepts at most seven contiguous ranges inside the selected range, rechecks access
between calls and stops on failure. The UI resolves these exact ranges from local calendar dates,
including daylight-saving changes and history clipping. Null aggregate results never become zero.

The service knows no Android/Compose classes, SDK implementation, DI container or screen.
ViewModels depend on it; the service depends only on domain models and the repository interface.
Health interpretation, scoring and diagnosis do not belong here.
