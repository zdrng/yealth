# Browser UI

`BrowserViewModel` owns date/period/search per navigation entry and persists only those
selections in `SavedStateHandle`. `CatalogLabels` maps all catalog types to localized labels.
`BrowserContent` contains the catalog; fictional records are provided only by an explicit
`preview=true` debug launch. Release has no fixture implementation.

`AccessViewModel` checks the domain service on foreground entry and after permission results.
It invalidates the environment on stop/recheck. `LiveRecordsViewModel` observes that state,
cancels reads and clears records/cursors on invalidation, and reads only while its metric or
associated detail is displayed. Access revision checks prevent stale UI during refresh.
All ViewModels receive `HealthBrowserService` by constructor and never import data/DI/SDK.

The metric and its detail share the metric entry's live ViewModel. Values/cursors are never
saved; after process recreation data is fetched again for the preserved selection. Records
beyond the first page can be loaded explicitly. Restoring an unloaded detail exposes reload/
paging instead of fabricating the record. Raw rows are individual lazy items; completed query
counts differ from partial loaded counts. M4 verifies this path against real Toolbox records.

`StepsSummaryViewModel` owns provider totals for Today, Activity and Steps. It is attached at
the screen level, never inside a lazy item, so scrolling does not restart reads. `SelectedRange`
resolves the same calendar range/history policy for summaries and raw records. Both revision
and selection identity guard against stale rendering. Live history charts remain M6.

M5 adds `OverviewViewModel` for visible Today/category summaries. It uses provider aggregates
where supported and at most one raw page per remaining visible-category type, explicitly marking
partial counts. Selection changes cancel old work; hiding/revoking access clears displayed data.
`StepsSummaryViewModel` loads seven exact provider day totals only for a detailed Week chart.
`RouteAccessViewModel` retains only the pending session ID across recreation; returned coordinates
remain in memory, are checked against current permission and clear on background. All nested raw
fields appear as separate lazy rows, including explicit nulls and empty lists.
