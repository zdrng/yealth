# M5 — Stable category coverage and Expressive UI

Verified on 2026-09-18/19 with the pinned Nix toolchain and API 36 ARM64 emulator.
M5 is complete; specialized VO₂max/resting-heart-rate charts remain M6, and the broader
cross-version/TalkBack/release-policy acceptance remains M7.

## Service and data coverage

- All **40 stable ordinary record types** in Health Connect client 1.1.0 have typed readers.
  The catalog's 41st entry, Mindfulness, was reviewed and remains an explicit experimental
  deferral to M8. No mindfulness permission is declared. The coverage matrix lists every type.
- `HealthBrowserService` gates raw reads and provider aggregates by availability, feature,
  implemented reader, exact read permission and optional full-history access. UI ViewModels
  depend on this service; SDK classes remain in `data/healthconnect`. Architecture tests pass.
- Sleep/exercise duration, distance and heart-rate summaries use provider aggregate metrics.
  Missing aggregates remain absent, and all origins/ranges remain in neutral domain models.
  Steps Week reads seven exact calendar-day provider totals, with DST-safe boundaries and
  explicit history clipping. It never adds raw records from potentially overlapping sources.
- Category overviews request at most one raw page per remaining selected-category type.
  Partial counts are identified; Browse counts catalog types rather than unqueried records.
  Hidden routes do not read; date/access changes cancel work and clear previous results.
- Raw optional nutrients, measurements, codes, metadata, samples and nested session fields
  remain browsable. Each nested leaf, explicit null and empty collection becomes a lazy row.
  Original timestamps/offsets and canonical units are retained.
- Exercise routes distinguish available locations, no route and consent required. The SDK
  per-session consent contract returns domain values. Pending IDs alone survive recreation;
  coordinates stay in memory and clear on background. Cancellation keeps the raw session
  visible and offers retry. No GPS/location permission is used.

## Design and review

A parallel **Astra** agent implemented reusable Material components, then refined them after
an independent **Terra / medium** screenshot review. The supplied references are retained in
`mockups/material-expressive/`; see [component/API decisions](m5-design.md).

The UI now uses compact icon-led summaries, category color roles, stronger type hierarchy,
connected expressive toggle controls, native loading indicators and rounded weekly bars.
Dynamic color remains enabled; the static fallback uses lilac/sage/rose roles. Only the chart
is custom data drawing. Raw controls, cards, navigation and interaction use Material APIs.

Terra's initial findings led to smaller category headings (fixing the “measurements” orphan),
more compact Today/Activity cards, semantic category colors and a single bar color without an
unexplained highlighted day. Final normal/dark captures had no visual blocker. Nutrition still
repeats a short empty-state description in its navigable type rows; this is minor copy repetition.

At **200% font scale**, Browse reflows to one column. Inspection found split chart numbers,
so exact values now move into full-width dated rows beneath the bars. A device assertion and
Terra's final visual review confirm that the values remain whole, readable and scrollable.
This is visual accessibility evidence, not a claim of completed M7 TalkBack/contrast auditing.

## Verification

- **63 Debug JVM tests and 54 Release JVM tests**, zero failures/errors.
- SDK inventory/schema checks cover all 41 catalog entries. Shared test-only SDK fixtures
  exercise every stable reader's field/time/metadata mapping, denied access, empty results and
  exact cursor paging. Separate tests cover optional nutrient null/zero, units, negative skin
  deltas, nested sessions/routes, provider aggregates and mixed origins.
- Overview tests cover selection cancellation, hidden routes, revocation, bounded queries and
  provider-summary failures. Weekly tests cover contiguous DST day ranges and absent versus zero.
- **16 distinct executed device test cases** across the following suites, plus theme/font repeats:

| Suite | Executed coverage | Result |
| --- | --- | --- |
| BrowserNavigationTest | Five navigation/search/date/restoration flows, all catalog routes, Expressive selection, varied preview chart | 5 passed |
| CategoryDeviceTest | All 40 stable SDK fixtures through real mappers/service/live UI; summaries/Week chart/date-specific Nutrition empty; route consent/cancellation | 3 passed |
| StepsDeviceTest with fixtures | Actual Toolbox source comparison, real provider paging/detail retention, Today→Activity→Steps and recreation | 3 passed; denial case intentionally skipped in this run |
| StepsDeviceTest with denial | Revoked Steps summary/records remain distinct from empty/zero | 1 passed |
| HealthAccessDeviceTest | Actual provider/settings, system request cancellation, provider/history recovery | 3 passed; opt-in grant-mutation scenario skipped |
| DesignDeviceTest | Today/Browse/Activity/varied Week screenshots using the labeled preview and static theme | 1 passed, repeated dark and at 200% |

The eight navigation/category cases passed together in 180.694 seconds. The real provider run
passed in 28.292 seconds. Final access/revocation run reported `OK (5 tests)` with four executed
and the grant-mutation scenario skipped. Dark and 200% font checks passed; after the numeric-row
fix the final 200% chart check passed again in 5.056 seconds.

The two existing Toolbox records (120 and 240 steps) were not modified. Real provider comparison
confirmed 360 for September 18 and the seven requested daily bins: the last day returned 360,
while the other six remained absent or recorded zero. Source evidence is saved separately from
the explicitly synthetic/injected SDK-fixture screenshots. The route cancellation UI uses an
injected domain response; a live provider route-sharing dialog was not exercised in this run.

## Build, permissions and footprint

The final command succeeded:

```sh
nix develop -c bash -c 'export ANDROID_USER_HOME="$HOME/.local/share/yealth/android"; ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleRelease :app:lintDebug'
```

- Lint: **0 errors**, 12 existing toolchain/dependency update notices; no new UI/resource warnings.
- No library/version changes. Six official Material vector icons are bundled instead of an
  extended icon dependency. Shared fixtures compile only into JVM/instrumentation tests.
- Manifest: **37 stable-reader read permissions + separate history access**; runtime requests
  remain category-scoped and feature-gated. Both merged manifests add only AndroidX's local
  protected receiver permission. No Internet, write, background or location permission.
- Backups remain disabled. Release mapping contains no fake repository, preview or fixture classes.
- Debug APK: **37,638,798 bytes**. Shrunk unsigned release: **3,520,922 bytes** (+129,196 vs M4).
- Normal app cold launch, default font/light theme and restored denied Steps/history permissions
  are checked after the tests. The emulator was stopped after capture. Existing synthetic Toolbox
  records remain for regression tests.

Evidence under `scratchpad/m5/` includes `final-preview/`, `final-device/`, `dark-reference/`,
`large-font-final/`, `live-provider/` and the suite result text files. Preview captures are labeled
sample data; injected live-UI fixtures are test artifacts, never a production fallback.

Primary API references reviewed alongside the pinned artifact:
[raw reads](https://developer.android.com/health-and-fitness/health-connect/read-data),
[provider aggregates](https://developer.android.com/health-and-fitness/health-connect/aggregate-data),
[sleep sessions](https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions),
[exercise-route consent contract](https://developer.android.com/reference/androidx/health/connect/client/contracts/ExerciseRouteRequestContract).
