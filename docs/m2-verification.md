# M2 — Expressive shell and navigation

## Scope

M2 replaces the Hello World screen with the navigable UI from the four mockups. The
Health Connect catalog and domain contracts remain the source of type identities; the UI
contains no Health Connect imports. Live permission requests and real record reads are still
M3/M4 work, and full readers/histories remain M5/M6.

The debug composition root supplies deterministic, fictional domain records from
`src/debug/.../ui/preview/PreviewContent.kt`. Every screen displays a sample-data banner.
The release composition root supplies only the catalog and an explicit disconnected state;
it has no sample records or fallback to fixtures. Access statuses in debug are labeled samples.
Neither variant requests permissions or reads Health Connect during the M2 flow.

## Architecture and navigation

- `BrowserViewModel` is scoped to each Navigation back-stack entry, receives its dependencies
  through its constructor, and exposes immutable `StateFlow<BrowserUiState>`.
- Compose observes state with `collectAsStateWithLifecycle` and sends selection/search events
  through ViewModel methods. ViewModels have no Context, repository implementation or DI imports.
- `SavedStateHandle` stores only date, period and query. Record payloads and pagination tokens
  are not persisted. Lazy list state is saved by Compose within each navigation entry.
- Three nested root graphs retain their back stacks using Navigation's `saveState` /
  `restoreState`. Child routes have stable category/type/record identifiers and preserve the
  originating root. Detail screens hide navigation and provide Back.
- Date intervals use calendar dates in the device's zone, with half-open instant boundaries;
  overlapping interval records are kept. No record values are aggregated by the UI.
- M2's `BrowserContent` is a presentation input for layout review, not a new production data
  source. M3/M4 will connect live state production to the existing `HealthBrowserService`.

## Component adaptations

All controls are public components of the pinned Material 3 `1.5.0-alpha14` library. There
are no custom navigation controls or mockup-shaped card substitutes.

| Mockup pattern | Implemented component / adaptation |
| --- | --- |
| Brand bar, large titles | `TopAppBar`, emphasized Material typography, theme tokens |
| Main navigation | `NavigationBar`; `NavigationRail` at widths ≥600 dp |
| Colorful cards | Clickable `Card`, `CardDefaults`, primary/secondary/tertiary containers |
| Category grid | Two columns when space permits; one at font scale >1.3 or narrow widths |
| Catalog search | `OutlinedTextField` with Search IME and clear action; local inline results, so no overlay SearchBar or additional search destination |
| Day/week/month; history periods | `PrimaryScrollableTabRow` + `Tab`; standard selected indicator, scrolls at large fonts |
| Date controls | `IconButton`, `FilledTonalButton`, `DatePickerDialog` + `DatePicker` |
| Raw data and provenance | `ListItem`, `Card`, full timestamps and actual offsets; source package and device are distinct |
| Access/privacy | `AlertDialog` with real informational actions; no nonfunctional permission buttons |
| History chart | Domain-content Canvas: individual dots at real timestamps, no interpolated trend; exact accessible raw rows below |

The mockup's Steps total is shown as an **individual record** until a real aggregate API is
connected. It never inherits a fabricated aggregate source/time. The mockup's “View in Health
Connect” action currently opens accurately labeled information about access; system destinations
and permission recovery belong to M3. Nutrition's empty state changes with the selected date.

## Dependencies

- Lifecycle Compose/ViewModel `2.9.4`: scoped ViewModels, saved selection, lifecycle-aware state.
- Navigation Compose `2.9.5`: nested graphs, independently saved tab stacks, system Back and
  saveable destination state. This stable 2.x line is compatible with the pinned Kotlin 2.1
  toolchain; M2 does not require a toolchain migration to Navigation 3.
- Material icons **core** under the existing BOM: standard navigation/action icons, without the
  extended icon collection.
- Compose UI test + AndroidX test runner/ext are instrumentation-test dependencies only.
- Manual constructor injection is retained; no DI framework, network SDK or database added.

Reference: [Google's multiple-back-stack guidance](https://developer.android.com/guide/navigation/backstack/multi-back-stacks),
[architecture recommendations](https://developer.android.com/topic/architecture/recommendations).

## Verification

Verified on 2026-09-18 using the API 36 ARM64 Yealth emulator and the pinned Nix environment.

- Debug and minified/resource-shrunk release builds pass.
- 29 debug JVM tests and 20 release JVM tests pass, including architectural boundaries,
  complete catalog-label coverage, selection restoration, localized search, interval/instant
  boundaries, leap dates and a daylight-saving transition.
- `lintDebug`: 0 errors, 12 warnings, all dependency-version update notices. No UI or resource
  warnings remain.
- Both merged manifests have no Internet or Health Connect read/write permissions, and backups
  remain disabled. The release mapping contains neither `ui.preview` nor `data.fake` classes.

| APK | M1 bytes | M2 bytes | Change |
| --- | ---: | ---: | ---: |
| Debug (normal emulator workflow) | 34,846,099 | 36,572,121 | +1,726,022 |
| Unsigned, shrunk release | 1,395,687 | 2,816,082 | +1,420,395 |

These changes include the newly retained UI, date-picker and navigation implementations, not
just dependency declarations. Debug tooling and fixtures are excluded from release.


### Device and visual acceptance

All five `BrowserNavigationTest` instrumentation tests pass on API 36:

- Every category and all 41 catalog metric/detail routes are reachable.
- Today → Steps → detail, focused detail navigation, previous/next day and date dialog dismissal.
- Search, independent tab stacks, selected periods, list position after detail Back, and activity
  recreation retain their state; originating tabs remain selected.
- Activity periods propagate into the chosen metric; Nutrition changes from empty to populated
  on the previous day; every access category and the privacy action opens its dialog.
- Selecting September 17 through the actual Material date picker updates the displayed date.

The three main flow tests also pass in dark mode. The layout/date-selection test passes at
system font scale **2.0**, and again with a **1800×2400** viewport that activates the navigation
rail. These are smoke checks, not a replacement for the full M7 device/TalkBack/contrast matrix.

Screenshots were visually compared with all four mockups. The category title layout was adjusted
so “Body measurements” fits naturally; paired cards have equal height. At large font scales the
category grid becomes a list. Screens, dialog buttons and navigation remained usable in the
checked configurations. Dynamic light/dark colors, official controls and focused details match
the planned hierarchy; decorative blobs and bespoke controls are intentionally absent.

Evidence is under the ignored `scratchpad/m2/` directory:

- `light/`: Today, Browse, Activity, Nutrition empty, Access, Steps detail, VO₂max and resting
  heart rate histories, and date picker.
- `dark/`: the same main flow screenshots in dark mode.
- `large-font/m2-adaptive-browse.png` and `large-font/m2-date-picker.png`: 200% font checks.
- `expanded/m2-adaptive-browse.png`: navigation rail and the expanded category layout.
- `light-results.txt`, `dark-results.txt`, `large-font-results.txt`, `expanded-results.txt`:
  successful AndroidJUnitRunner output (5 / 3 / 1 / 1 tests).

Inside the Nix shell, reproduce the build checks with:

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:testDebugUnitTest :app:testReleaseUnitTest :app:assembleRelease :app:lintDebug
```

Then run `scripts/emulator.sh test` on a started development emulator. The instrumentation
suite captures screenshots to the app's external-files directory. Direct AndroidJUnitRunner runs
were used for the theme/font/width screenshot captures so the files can be pulled before test
package cleanup. Screenshots contain only synthetic records.

The normal `scripts/emulator.sh smoke` developer workflow also passed: build, installation,
cold launch (`Status: ok`) and a live process check. The emulator was returned to font scale 1.0,
its default size and light mode, with the app signed by the normal emulator-workflow debug key.
The test package was removed and the emulator stopped after verification.
