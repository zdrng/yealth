# Data prominence — 19 September 2026

User follow-up: opening a metric should emphasize its quantity, like the supplied Google
Steps reference. Implemented compact metric app-bar titles, reusable emphasized measurements
with smaller units, prominent provider totals/averages, individual-record heroes, and a
taller weekly chart before query/source notes. No new provider calculation, permission,
dependency, persistence or milestone M6 functionality was introduced.

## Checks

- Debug, shrunk Release and Android test APK builds passed. Lint: zero errors; the same
  12 dependency-update warnings as before. Build and device logs are retained alongside
  the screenshots in `scratchpad/data-prominence/`.
- 63 Debug JVM tests: passed, zero failures/errors/skips.
- `CategoryDeviceTest`: all 3 cases passed, including opening all 40 stable record types,
  raw detail/route navigation, weekly provider chart and date-specific empty state.
  Existing summary navigation additionally asserts the metric pages retain the provider's
  72 bpm average and 444 min sleep total from test fixtures.
- `BrowserNavigationTest`: all 5 cases passed, including all catalog routes, selected periods,
  date picker, adaptive layout and detail back navigation.
- `DesignDeviceTest`: passed in light, dark and dark with 200% system font. Captures the initial
  metric page, weekly plot, exact large-font chart values, and opened individual record.
- Astra implemented the structured record component. Terra independently reviewed actual
  normal and enlarged-font screenshots: data hierarchy accepted, no clipping found.

Device evidence: `scratchpad/data-prominence/light/metric-first-view.png` and
`record-first-view.png`, corresponding `dark/` and `large-dark/` images, and production-layout
fixture screenshots in `live/`. Previews and injected SDK fixtures are synthetic test data;
no screenshot is evidence of a user's actual measurements.

The preview headline is explicitly the latest individual record, not a fabricated weekly
average. Production Steps uses Health Connect's range total and daily aggregate bins.
Session interval length is labeled separately from provider duration totals. Other scalar
and series heroes read typed source values and keep units and sample-time identity.

At enlarged fonts, the screen scrolls to show the full chart and original dated value rows.
Underlying fields and metadata remain available, including exact raw timestamps and offsets.
