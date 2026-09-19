# M4 — Complete Steps slice

Implementation date: 2026-09-18. No new dependencies or permissions.

## Behavior and architecture

Today and Activity show the provider's Steps total for the selected range. Today also exposes
an Activity entry preserving the selected date. Activity carries Day/Week/Month into the Steps
list. The metric screen separates the aggregate from individual records and their details.
All cards, tabs, pickers, navigation, buttons and lists use the existing official Material 3
Expressive components. Overview totals have compact date labels; metric totals include exact
query boundaries, UTC offsets and contributing app packages returned by the provider.

`HealthRepository.stepsTotal` returns a neutral `StepsTotal`; only `data/healthconnect` uses
`AggregateRequest`, `StepsRecord.COUNT_TOTAL` and `AggregationResult`. `HealthBrowserService`
checks provider, reader, permission and history access before either raw or aggregate reads.
No raw-record sum substitutes for the provider total. Null totals remain absent; actual zero
remains zero. Provider errors, denied access and unsupported history remain distinct states.
The existing architecture tests enforce SDK isolation and lower-layer independence.

`StepsSummaryViewModel` is scoped to the screen's navigation entry, outside lazy list items.
Scrolling cannot start or cancel summary requests. Hidden screens detach; foreground/access
changes and selection changes cancel outstanding work and clear old values. Health values
and cursors are never persisted. Access revisions and selected-range identity guard rendering
while newer work is pending. Sharing a records ViewModel between metric and detail no longer
restarts reads merely because the number of attached screens changes from one to two.

`selectedRange` is shared by raw reads and summaries. Day is local midnight to next local
midnight; Week is the selected date plus the previous six dates; Month is the rolling calendar
month ending on the selected date. All provider requests are half-open instant ranges. Local
midnights account for DST (23/25-hour days); default zone is re-resolved on foreground refresh.
Original record intervals and offsets are preserved, including portions outside the range.
Without history, the existing conservative recent-window policy remains explicitly labeled.

Records are now separate lazy list items. Paging retains unique record IDs and the exact query
bound to each token, handles empty intermediate pages, cancels old page requests on selection
changes, and distinguishes loaded partial counts from completed query counts. Retry re-reads
Health Connect; source changes can be reflected by refreshing. The provider does not promise
a frozen multi-page snapshot while another app edits its records.

## Source fixture setup

Device: project API 36 Google APIs ARM64 emulator, Europe/Berlin. Official Google Toolbox
2.3.5 was downloaded from the link in the developer documentation and installed as
`androidx.health.connect.client.devtool`. Yealth remained read-only. Only Toolbox wrote these
synthetic records; fixture writes are not part of Yealth or its release manifest.

On 2026-09-18, Toolbox Insert Health Record → Activity → Steps created two 30-minute records:

| Count | Start (Europe/Berlin) | Source / offsets |
| --- | --- | --- |
| 120 | 22:26:27.318 | Toolbox, +02:00 at both ends |
| 240 | 20:00:04.049 | Toolbox, +02:00 at both ends |

The expected full-day total is 360; both raw values must remain independently browsable.
The first insert's aggregate was briefly absent while the provider updated; refreshing later
returned 120, also visible in Health Connect's own source-priority screen. No synthetic fallback
or automatic conversion of absent totals to zero was added.

The exact returned IDs, timestamps, metadata and aggregate are captured in
`scratchpad/m4/light/m4-source-comparison.txt`. Input/save screenshots and the Health Connect
120-step total are in `scratchpad/m4/`. These are synthetic test evidence, not production logs.

## Verification

- Debug JVM: 52 tests passed. Release JVM: 43 tests passed.
- New coverage: exact SDK aggregate metric/range, integral values, absent versus zero totals,
  all contributing origins, error redaction/cancellation, permission/provider/history gates,
  DST/leap-month/zone boundaries, summary cancellation/retry/hidden routes, empty intermediate
  pages, explicit refresh of an unchanged query, detail/back page retention, ID deduplication and cancellation of a pending page after changing the range.
- Debug and R8/resource-shrunk release builds passed. Lint: zero errors; 12 existing dependency
  and toolchain update notices. No runtime/test library was added.
- Four distinct M4 device cases passed: full Today/Activity/Steps/detail flow with month changes
  and Activity recreation; real two-page provider/source comparison; one-record page loading
  with the second record retained across detail/back; and denied/revoked summaries and rows.
  The full flow also passed at 200% font size in dark mode. Screenshots were visually reviewed.
  Fixture runs skip the separate denied-access case; revoked runs select it explicitly.
- Debug/release merged manifests retain exactly four health read permissions, no Internet,
  write or background-read access, and backup disabled. Release mapping contains no fixtures.
- Final APK sizes: debug **38,085,336 bytes**, unsigned shrunk release **3,391,726 bytes**.
  Relative to M3: +763,432 debug / +70,856 release bytes. No new dependencies.

Evidence: `scratchpad/m4/device-results.txt`, `large-font-results.txt`,
`revocation-results.txt`; screenshot folders `light/` and `large-font/`.
M3 access regression: three ordinary device cases passed (the separate grant-mutation case
was skipped). The five M2 navigation regression cases also passed, including all 41 catalog routes.
Together these runs executed 12 distinct device cases, plus the large-font repeat.

Reproduction (inside `nix develop`, with emulator and the two documented fixtures present):

```sh
export ANDROID_USER_HOME="$HOME/.local/share/yealth/android"
./gradlew :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
adb shell am instrument -w -e stepsFixtures true -e class dev.zdrng.yealth.StepsDeviceTest dev.zdrng.yealth.test/androidx.test.runner.AndroidJUnitRunner
```

Fixture tests are opt-in and require Steps read access; they never write health records.
`liveProviderPagingAcrossDetail` changes only the test wrapper's page size to one, using the
real repository/provider and production UI/ViewModels. Ordinary builds retain page size 100.
`deniedSummariesAndRecords` is separately selected with `-e stepsDenied true` after revocation.

## Primary references

- [Health Connect aggregation](https://developer.android.com/health-and-fitness/health-connect/aggregate-data):
  use provider aggregates for cumulative Steps and its source-priority handling.
- [Toolbox](https://developer.android.com/health-and-fitness/health-connect/test/health-connect-toolbox):
  official fixture writer and APK distribution.
- [Integration test cases](https://developer.android.com/health-and-fitness/health-connect/test/test-cases):
  compare source records, reading, access denial and revocation.

Public API names/signatures were also checked against the pinned Health Connect client 1.1.0
artifact with `javap`; no rolling-documentation API upgrade was needed. Cross-device/provider
release coverage, full TalkBack testing and the remaining 38 record readers remain later
milestones. M4 does not claim all 41 catalog types are implemented.

Emulator font size and light mode were restored and Yealth Steps/history permissions revoked
after verification. The two synthetic Toolbox records remain available for future regression
checks. The project emulator was stopped after the final normal-launch smoke check.
