# Yealth plan

## Purpose

Yealth is an offline, read-only Health Connect browser. It displays health information as
provided by Android without editing, medical advice, scoring, interpretation, prediction or
cloud synchronization. The intended experience is complete, calm and modern, using Jetpack
Compose and Material 3 Expressive.

This repository contains the verified scaffold, domain/service contracts and typed catalog,
progressive Health Connect access, and M5 stable-record coverage with Expressive browsing UI.
Normal debug and release builds use live access. An explicit debug preview launch exposes
labeled sample records. The images in `mockups/` define the information architecture, content
hierarchy and navigation, while the added Material references guide expressive presentation.
They are not pixel-perfect specifications and do not prescribe custom controls.

## Fixed decisions

- Application ID and namespace: `dev.zdrng.yealth`
- Android language/UI: Kotlin and Jetpack Compose
- Minimum Android version: Android 9 / API 28
- Compile and target SDK: API 36
- Design system: Material 3 Expressive
- Product UI must use official Material 3 Expressive components exclusively; mockup-specific
  controls, cards, navigation elements or decorative shapes must not be recreated as custom
  substitutes for Material components
- Compose Material 3 is temporarily pinned to `1.5.0-alpha14`, matching the proven Hablock
  baseline, because the required Expressive theme, motion and emphasized typography APIs are
  not public in stable Material 3 `1.4.0`; review and repin deliberately as the API stabilizes
- Dynamic Material You colors on Android 12 / API 31 and newer
- Static Yealth light/dark schemes as fallback on API 28–30
- Edge-to-edge layout
- No Internet permission and no network stack
- Read-only Health Connect access; no write permissions
- Historical access is in scope via `READ_HEALTH_DATA_HISTORY`
- No background health reads
- No FHIR / Personal Health Records in version 1
- Health Connect remains the source of truth; health records are not copied into a local database
- Room/DataStore may be added only for small local UI state if a concrete requirement appears
- Manual dependency injection; no Hilt or other DI framework
- One Gradle module initially, with package boundaries enforced by tests
- R8 code shrinking and Android resource shrinking for release builds

## Theme behavior

The application theme follows this decision:

```kotlin
val colorScheme = when {
    supportsDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
    supportsDynamicColor -> dynamicLightColorScheme(context)
    darkTheme -> YealthDarkColorScheme
    else -> YealthLightColorScheme
}
```

Expressive components, shapes, type and motion work throughout the supported API range.
Wallpaper-derived dynamic color is available only on Android 12 and newer. Earlier versions
receive equivalent static light and dark schemes. M5 provides coordinated lilac, sage and rose fallback roles, including surface containers.

## Product UI and mockup interpretation

The files in `mockups/` are structural references rather than visual specifications. They may
guide which information belongs on a screen, the relative prominence of that information, the
navigation flow and the broad grouping of records. Exact dimensions, colors, typography,
illustrations, gradients, organic containers and component shapes from those images are not
implementation requirements.

All interactive and structural UI must be composed from official Material 3 Expressive
components available in the selected Compose Material 3 version. This includes navigation,
app bars, buttons, icon buttons, button groups, cards, list items, tabs, search, date and range
selection, dialogs, sheets, progress and loading states. Prefer the component defaults and
public customization APIs for color, typography, shape, elevation, motion and state.

Do not reproduce a mockup element with a custom-drawn control when an applicable Material 3
Expressive component exists. Do not create a parallel design system or fork Material behavior.
Custom composables remain appropriate for arranging domain content and rendering data for which
Material has no component, such as health charts, record-value layouts and empty-state artwork.
Those composables must use Material theme tokens, accessibility semantics and standard Material
interaction behavior; they must not imitate a new control type.

When a pictured pattern has no official expressive equivalent in the pinned library, adapt the
screen to the closest official component instead of pursuing pixel parity. If a required
expressive component exists only in a newer Material 3 release, review and deliberately update
the dependency rather than implementing a private replacement.

### M5 design direction (user clarification)

The mockups remain structural references, not pixel specifications. The app must nevertheless
feel playful, deliberately designed and visibly Expressive. The user-supplied references in
[`mockups/material-expressive/`](mockups/material-expressive/) guide stronger type hierarchy,
compact icon-led cards, connected selection controls, generous shape variation and purposeful
color roles. Use official Material components and their public customization APIs. Dynamic
colors remain supported; the reference palette is not forced over wallpaper preferences.

The rounded Steps bars are an explicit design direction. The Week metric screen displays
seven calendar-day provider totals with a zero baseline, visible missing values and accessible
numeric labels. Do not import the reference's goals/checkmarks/health judgments. Charts remain
domain content, not custom controls. Technical range/provenance details belong in the detail
hierarchy, not dominant overview cards. Visual acceptance requires actual device screenshots
and review of density, emphasis and large text, alongside functional tests.

### Detail data emphasis (19 September follow-up)

The additional [Google reference](mockups/material-expressive/data-prominence.jpg) clarifies
the hierarchy after opening a metric: a compact app-bar title, a dominant measurement with
a smaller unit, then the graph. Range controls remain usable; provenance follows the data.

- [x] Reusable large measurement presentation for opened metrics and individual records.
- [x] Preserve provider totals/averages on supported metric pages; explicitly label individual
  records, latest samples and session interval lengths elsewhere.
- [x] Remove competing large detail headings; enlarge the weekly plot and move technical
  query bounds below it.
- [x] Verify actual device layouts, navigation and enlarged fonts after this refinement.
  See [data-emphasis verification](docs/data-prominence-verification.md).

## Architecture

The code follows unidirectional data flow:

```text
Compose screen -> ViewModel -> domain service -> repository -> Health Connect data source
       ^                                                        |
       +---------------- immutable UI state --------------------+
```

### `domain/`

Pure Kotlin. It owns neutral display models, repository contracts and services that coordinate
availability, permissions, time ranges and pagination. It must not import Android, Compose,
Health Connect implementations, `data`, `ui`, or `di`.

The service layer does not evaluate health. It may perform lossless operations required for
display, such as unit labeling, paging, grouping records by declared Health Connect category,
or selecting fields for a detail view.

### `data/`

Owns data-source implementations. `data/healthconnect` is the only place that directly uses
Health Connect SDK record classes. It checks feature availability, executes reads, handles page
tokens and maps records into domain models. `data/local` is reserved for justified local state.

### `ui/`

Owns Compose screens, reusable components, navigation, theming and screen-level ViewModels.
Composable functions render immutable state and send events upward; they never call Health
Connect directly. Product features should be grouped below `ui/feature`.

### `di/`

`AppContainer` is the composition root. It constructs concrete repositories and services and
provides them to ViewModels. This preserves testability without adding a DI dependency.

## Health Connect scope

Health Connect has no generic "read everything" endpoint. Each record type is a compile-time
type with an explicit permission, schema and query. Implementation will therefore use a typed
`HealthRecordCatalog`. Each catalog entry will describe:

- stable internal ID and Health Connect category;
- record class and required read permission;
- feature gate where required;
- supported raw fields, units and metadata;
- time semantics: instant, interval or series;
- reader/paging adapter;
- display formatter that does not infer health meaning.

Version 1 targets ordinary health and fitness records: activity, body measurements, cycle
tracking, nutrition, sleep, vitals and wellness where supported by the pinned stable Health
Connect SDK. Experimental types must be individually reviewed before inclusion.

### Complete-data browsing requirement

Yealth must offer comprehensive browsing of the data Health Connect makes available to the app,
in the spirit of Apple Health's ability to browse all recorded data. The mockups show example
screens, not a whitelist of supported metrics. Coverage includes every ordinary record type in
the pinned stable SDK, every returned record/page in the permitted range, all available raw
fields and units, individual series samples, nested session details and source metadata.
Optional fields remain distinguishable from zero. Summaries and charts must always lead to raw
records rather than hiding data that lacks a bespoke dashboard card.

"All data" is bounded by the Android APIs, provider/device support, explicit user permissions
and available history. It does not promise access to data a source app never writes to Health
Connect or identical data availability to Apple's platform. Missing permissions, unsupported
features, experimental APIs and not-yet-implemented readers must be shown or documented
explicitly, never silently omitted or presented as no records.

Maintain the [record coverage matrix](docs/health-connect-coverage.md) against the actual pinned
SDK artifact. Every SDK upgrade must reconcile newly added record types, fields and feature
gates. M5 cannot be accepted with an unexplained gap in ordinary stable-record coverage.

FHIR Personal Health Records remain outside version 1 but are explicitly tracked in M8 toward
the full-data product goal. They cover clinical resources such as conditions, medications,
allergies, lab results and encounters, using a separate API with its own stability, permission
and release requirements. Experimental ordinary records must also have an explicit review
decision; deferral must be visible in the coverage matrix.

## Permissions and privacy

The final manifest will declare only read permissions for record types actually implemented.
Manifest declarations do not grant access. Runtime requests should be progressive and tied to a
visible category or feature rather than showing a blanket request on first launch.

The application will:

- explain why a category needs access before requesting it;
- work when access is partial, denied or later revoked;
- expose a direct route to Health Connect access management;
- read only while visible;
- request historical access separately and explain the older-than-30-days purpose;
- never request Internet, background-read, write, storage, location, sensor or notification
  permissions unless a future user-facing requirement is approved first;
- avoid logging record contents or personally sensitive values;
- keep backups disabled by default.

Google Play requires a specific user-facing justification for every requested Health Connect
data type. The release permission list must therefore match implemented and visible screens.

## Data presentation rules

- Show source values faithfully with their units, timestamps, zone offsets and data origin when
  available.
- Distinguish no data, no permission, unsupported feature, unavailable Health Connect and read
  failure; never collapse these into a misleading zero.
- Series records expose their samples without deriving trends or diagnoses.
- API-provided aggregates may be displayed only when clearly labeled as Health Connect totals,
  averages, minima or maxima. Yealth does not create proprietary scores.
- Formatting is a UI concern; the underlying value and unit remain available in domain models.
- Accessibility, large fonts, TalkBack semantics and adaptive layouts are acceptance criteria.

## Development environment

The Nix flake pins JDK 17, Android SDK/platform 36, build-tools 36.0.0, an API 36 Google APIs
emulator image, adb, scrcpy, GNU Make, Python and ShellCheck. It supports Apple Silicon macOS,
Intel macOS and x86_64 Linux/NixOS with the host-native emulator ABI.

Common commands:

```sh
make build          # debug APK
make test           # JVM tests
make test-scripts   # emulator orchestration tests without an SDK
make run            # windowed emulator, build, install and launch
make run-headless   # headless equivalent
make screenshot
make logs
make stop
```

Mutable emulator state lives outside the immutable Nix SDK under
`${XDG_DATA_HOME:-$HOME/.local/share}/yealth/android`. The default AVD and port can be changed
with `YEALTH_AVD` and `YEALTH_EMULATOR_PORT`.

## Mockup screen inventory and UI flow

The following flow is the implementation contract derived from all four mockups. Screen labels
below follow the references; production copy belongs in localized string resources. Sample
values, dates, sources and record counts in the images are fixtures, never production defaults.

| Reference | Screens and hierarchy | Planned official Material components |
| --- | --- | --- |
| [Home and Browse](mockups/yealth-home-browse.png) | Today: date, Health Connect introduction, Steps/Sleep/Heart rate summaries. Browse: search, category overview. | `Scaffold`, top app bar, `NavigationBar`, `NavigationRail` at expanded widths, `Card`, `ListItem`, `SearchBar`, date picker, icon buttons. |
| [Activity and detail](mockups/yealth-activity-detail.png) | Activity: Day/Week/Month, date navigation, Steps/Exercise/Distance. Detail: value, time, source, available metadata, Health Connect action. | Official single-choice button group or tabs, date picker, `Card`, `ListItem`, `Button`, `IconButton`. |
| [Access and empty state](mockups/yealth-access-empty-state.png) | Access: category permission status, management action, privacy note. Nutrition: selected day, empty explanation, choose another date. | Top app bar, `ListItem`, `Card`, buttons, date picker, rationale dialog or sheet. |
| [VO₂max and resting heart rate](mockups/yealth-vo2max-resting-heart-rate.png) | Metric history: 7/30/90 days or 1 year, recorded-values chart, raw rows, record count and provenance note. | Official single-choice button group or tabs, `Card`, `ListItem`, dividers and themed text; chart drawing is domain content. |

The component inventory is a planning baseline. Verify public APIs against the pinned Material
library while implementing M2; record the exact chosen component and any adaptation here.
Use Expressive defaults and theme tokens, including emphasized typography and expressive motion.
Do not reproduce the mockups' organic card outlines, gradients or custom navigation pill.

```text
Launch -> Today
Main navigation: Today <-> Browse <-> Access
Today -> select date -> summary -> metric records/history -> individual record detail
Browse -> category (or search result) -> record type/list -> individual record detail
Browse -> Activity -> Day/Week/Month + date -> Steps / Exercise / Distance
Browse -> catalog category -> VO₂max or Resting heart rate -> history -> record detail
Category/list without records -> choose another date -> same category/list
Missing access -> category rationale -> system permission request -> return to originating screen
Access -> category rationale or Health Connect access management -> refreshed permission status
History beyond permitted range -> separate history rationale/request -> retry selected range
```

Top-level destinations retain their own navigation state. Back returns to the previous screen
with its selected date/range, query and list position. Keep navigation visible on category and
history screens; individual record details use a focused screen with a back action, as in the
Activity/detail reference. The selected navigation item always reflects the originating root.
Correct the Access mockup's inconsistent Browse highlight: Access must be selected there.
The settings/overflow affordance leads to access/privacy information using defined actions only.

All supported catalog categories remain in scope, including cycle tracking even though it is
absent from the overview image. Permission requests remain tied to implemented record types.
Search filters catalog/category/record-type labels locally; it does not trigger blanket reads
of all health records. Counts represent the selected range and must identify partial results.

Record details and aggregate summaries are distinct. A Health Connect total must state its
range and cannot inherit a single record's timestamp or source. Show an individual record's
actual time semantics and available origin/device metadata. Do not invent an IANA zone name
from an offset, assume a source app is a device, or display "synced by source" without evidence.
The "View in Health Connect" action must use a supported destination; if record-specific linking
is unavailable, label the action for the settings/data destination it actually opens.

## Delivery milestones

Checkboxes track implementation and verification separately. `[x]` means the stated deliverable
was verified; existing scaffold items below were confirmed by source inspection. Build/device
acceptance remains open until executed and recorded. A milestone is complete only when all its
tasks and acceptance checks are complete. Update these boxes in the same change as the work.

- [x] **M0 — Scaffold accepted:** existing foundation verified by build and device checks.
- [x] **M1 — Data contracts and availability:** testable domain and Health Connect integration boundary.
- [x] **M2 — Expressive shell and navigation:** complete mockup flow with isolated preview/test fixtures.
- [x] **M3 — Access and history permissions:** progressive access and recovery flows work on device.
- [x] **M4 — First complete data slice:** Today → Activity → Steps → detail backed by real reads.
- [x] **M5 — Category coverage and empty states:** implemented catalog exposed through Browse.
- [ ] **M6 — Metric histories:** VO₂max and resting heart rate with faithful chart/list views.
- [ ] **M7 — Accessibility, hardening and release:** acceptance matrix and release audit complete.
- [ ] **M8 — Post-v1 complete-data expansion:** clinical/FHIR resources and reviewed SDK additions.

Acceptance evidence for M0/M1 is recorded in [the verification report](docs/m0-m1-verification.md).
M2 evidence is recorded in [the UI verification report](docs/m2-verification.md).
M3 evidence is recorded in [the access verification report](docs/m3-verification.md).
M4 evidence is recorded in [the Steps verification report](docs/m4-verification.md).
M5 evidence is recorded in [the category/design verification report](docs/m5-verification.md).
The next implementation milestone is M6.

### M0 — Scaffold acceptance (complete)

- [x] Gradle wrapper, version catalog and pinned Nix environment are present.
- [x] Emulator lifecycle tooling and script tests are present.
- [x] Hello World uses `MaterialExpressiveTheme`, expressive motion and emphasized typography.
- [x] Dynamic light/dark colors and static fallback schemes are wired into the theme.
- [x] Package boundaries, manual `AppContainer` and a domain architecture test are present.
- [x] Source manifest declares no health/network permissions and disables backups.
- [x] Complete and record every check in "Definition of done for the scaffold" below.

### M1 — Contracts and availability (complete)

Depends on M0. Keep domain models independent of Android and Health Connect SDK types.

- [x] Review and pin the stable Health Connect client with dependency and build-size notes.
- [x] Define neutral category, record, source, unit, instant/interval/sample and page models.
- [x] Define repository/service contracts for availability, feature support, permissions,
  selected time range, record reads and pagination.
- [x] Implement the typed `HealthRecordCatalog` with explicit adapters and feature gates;
  maintain a record-type/permission/visible-screen coverage matrix.
- [x] Model loading, data, empty, missing/partial access, unsupported feature, unavailable or
  update-required provider, and read failure as distinct immutable UI states.
- [x] Wire implementations through `AppContainer`; add a fake repository for previews/tests
  with deterministic states and no fallback to fake health data in production.
- [x] Acceptance: JVM tests cover model mapping, availability, feature gates and state handling;
  architecture checks enforce SDK isolation in `data/healthconnect` and UI/domain boundaries.

M1 registers all 41 ordinary SDK record types and supplies three representative typed readers
(Steps, HeartRate, VO₂max) to verify interval/series/instant mappings and paging. Catalogued
types without readers explicitly return `NotImplemented`; full read coverage and visible
permission requests remain in M3–M6. M1 itself added no health permissions; M3 adds only the three visible readers and optional history access.

### M2 — Material 3 Expressive shell and mockup navigation (complete)

Depends on M1. Fixtures allow flow review before completing every read adapter.

- [x] Replace Hello World with Today/Browse/Access destinations and screen-level ViewModels.
- [x] Implement category, record list, history and record detail routes with stable IDs;
  preserve originating destination, date/range, query and scroll position on return.
- [x] Implement the screen/component inventory above using public APIs in the pinned version;
  document adaptations before considering a deliberate dependency update.
- [x] Build Today hierarchy: date selection, offline/read-only introduction and summary cards.
- [x] Build Browse hierarchy: local catalog search, category cards and catalog-backed routing.
- [x] Build category/detail layouts and the access/privacy entry point without dead controls.
- [x] Apply theme colors, Expressive typography/shapes/motion, edge-to-edge insets and adaptive
  navigation; use standard Material interactions and visible selected states.
- [x] Acceptance: exercise every route with fixtures, including back and tab switching;
  review screenshots against the mockups' information hierarchy in light and dark themes.

M2 uses `OutlinedTextField` for inline catalog search and `PrimaryScrollableTabRow` for
period selection. Navigation Compose saves each root stack; record details hide navigation.
All 41 catalog routes were exercised using debug-only fixtures. M2 originally shipped a
disconnected release shell; M3 connects the three implemented raw-record readers. See the report for component adaptations, screenshots, dependency
size review and the live-access boundary.

### M3 — Progressive access, provider recovery and history (complete)

Depends on M1 and M2. Add each manifest read permission only with its implemented visible slice.
M3 connects the existing Steps, HeartRate and VO₂max adapters to raw list/detail screens so
access has an observable use. Today/Activity summaries, aggregate values and the complete
Steps source-data acceptance remain M4. Other catalog types remain explicitly unavailable.

Implementation decisions: activity-result permission contract and SDK intents stay in the
Health Connect adapter, wired through the composition root. Access/record ViewModels depend
only on the domain service. Foreground rechecks invalidate in-memory records and cursors;
hidden routes do not restart reads. Selection/navigation state survives permission dialogs.
Without history access the UI explicitly labels a conservative last-30-days query, never
claims that now-minus-30-days is the original grant boundary, and offers separate history
consent. Full older selections retry automatically after a history grant.

- [x] Build Access with granted, partially granted and not-granted category states, plus the
  offline/read-only/no-cloud explanation and Health Connect management action.
- [x] Explain category access before the system request; return to the originating screen
  after grant, denial or cancellation without losing its selected date/range.
- [x] Recheck permissions on foreground return; remove stale sensitive UI after revocation
  and stop/cancel reads when the app is no longer visible.
- [x] Implement unavailable/update-required provider and unsupported-feature recovery actions.
- [x] Implement the separate `READ_HEALTH_DATA_HISTORY` rationale/request for older data;
  gate it by support and preserve usable recent data when history access is denied.
- [x] Acceptance: test partial grant, denial, cancellation, revocation, provider recovery and
  unavailable history access; no permission or provider failure appears as zero/no records.

### M4 — First complete slice: Today → Activity → Steps → detail (complete)

Depends on M3. This establishes the reusable read, paging and display path.

- [x] Implement the Steps read adapter, raw record mapping and page-token handling.
- [x] Implement Day/Week/Month selection, previous/next range and date picker in Activity;
  define range boundaries consistently across local dates, offsets and daylight-saving changes.
- [x] Connect Today and Activity summaries to real data; use labeled Health Connect aggregates
  only where supported, retaining their range and distinguishing them from raw records.
- [x] Connect the Steps list and individual detail with raw value/unit, interval, available
  offsets, source and metadata; implement the correctly labeled Health Connect action.
- [x] Prevent stale results after changing date/range; support retry and incremental paging
  without duplicate or missing rows or misleading full-result counts.
- [x] Acceptance: complete the flow with Health Connect Toolbox fixtures on device; verify
  values, aggregate labels, range boundaries, paging, denial and revocation against source data.

M4 uses `StepsRecord.COUNT_TOTAL`, preserving the queried range, nullable result and provider
origins. Summaries never sum raw records or inherit a record timestamp/device. Day uses local
midnights, Week the trailing seven dates, and Month the rolling calendar month ending on the
selected date; previous/next shift that selection by the chosen period. Summary reads belong
to visible screen ViewModels, independently of lazy list composition. Two Toolbox records
(120 and 240 steps) verified the 360-step total, raw details and real provider pagination.
Device checks include incremental loading, detail/back retention, Activity recreation,
200% fonts, dark mode and revoked access. No dependencies or permissions were added.

### M5 — Remaining categories, search and empty states (complete)

Depends on M4. Deliver each category as a reviewable adapter + permission + UI + test slice.

- [x] Complete Activity slices, including exercise sessions and distance.
- [x] Complete Sleep slices and the Today sleep summary.
- [x] Complete Vitals slices, including heart-rate samples and the Today heart-rate summary.
- [x] Complete Body measurements slices.
- [x] Complete Nutrition slices and the pictured date-specific empty state.
- [x] Complete Cycle tracking slices.
- [x] Complete Wellness slices where supported by the selected SDK and feature gates.
- [x] Reconcile every in-scope stable record type with the coverage matrix; document reviewed
  experimental exclusions and unsupported types rather than silently omitting them.
- [x] Expose all returned raw fields, optional nutrients, nested session details, samples and
  source metadata through browsable records, including types without a bespoke summary card.
- [x] Handle exercise-route consent separately from absent route data; do not request device
  location access to read routes supplied through Health Connect.
- [x] Connect Browse search/category routes and range-aware counts to the completed catalog;
  display mixed sources accurately and avoid claiming a single source for mixed records.
- [x] Implement separate loading, empty, access, unsupported and error presentations throughout;
  "Choose another date" changes the active category range without losing navigation context.
- [x] Acceptance: every enabled catalog entry reaches its list/detail and has mapping, unit,
  permission, empty-result and paging coverage; series expose their original samples. Verify
  the complete-data requirement against all ordinary stable SDK types and their field schemas.

M5 enables all 40 stable ordinary SDK readers, preserving nullable fields, units, nested
sessions and metadata. Mindfulness is explicitly reviewed and deferred to M8 because the
pinned SDK marks it experimental; no unsupported Wellness reader is silently enabled.
The service also supplies Sleep/Heart/Distance/Exercise summaries and exact seven-day Steps
bins. Exercise-route consent is separate from absent data and uses no device location access.
Astra implemented the reusable Expressive UI; Terra reviewed real screenshots, requested density
and wrapping refinements, and accepted the updated normal/dark/200% font captures. Full-width
chart value rows preserve readable numbers at large fonts. See [M5 verification](docs/m5-verification.md)
and [design decisions](docs/m5-design.md) for executed checks and remaining M7 acceptance limits.

### M6 — VO₂max and resting-heart-rate history

Depends on M5 and the history-access flow in M3.

- [ ] Implement catalog-backed routes to both metric history screens.
- [ ] Implement 7/30/90-day and 1-year selection with the official Material choice component;
  preserve selection and handle partially accessible history explicitly.
- [ ] Render recorded values with labeled date/value axes and units using Material tokens;
  preserve gaps and source values without smoothing, predictions, scores or health judgments.
- [ ] Provide accessible raw rows containing date/time, value/unit and actual source, linking
  each row to its record detail; chart values remain available without touch-only interaction.
- [ ] Add truthful loaded/total record counts and the "values shown as provided" note.
- [ ] Acceptance: chart points and rows match the same source records; verify empty, single
  value, multiple sources, paginated data, large fonts and history-permission denial.

### M7 — Accessibility, hardening and release

Depends on M0–M6. Record commands, devices/API levels and evidence with acceptance results.

- [ ] Verify compact/expanded widths, rotation, system back, process recreation and range/query
  restoration without persisting health records or adding unjustified local storage.
- [ ] Verify large fonts, TalkBack traversal/labels, touch targets, contrast, non-color status
  cues and accessible alternatives for charts; review all distinct UI states.
- [ ] Test Android 9 fallback themes/provider availability, Android 12 dynamic colors and
  current Android/Health Connect, including light/dark and forced static color.
- [ ] Run JVM, script and device tests, including provider update/unavailability, revoked
  access, foreground-only reads, time-zone changes and daylight-saving boundaries.
- [ ] Audit the release merged manifest and dependencies: only implemented read permissions,
  no Internet/network stack, background reads or write permissions; backups remain disabled.
- [ ] Audit logs and persisted state for health values; verify no health database or uploads.
- [ ] Build with R8/resource shrinking, smoke-test the release APK and record its size.
- [ ] Publish the privacy policy and complete Health Apps/Play permission declarations with
  a user-facing justification matching each visible implemented data type.
- [ ] Acceptance: every milestone and release check has evidence; unresolved failures remain
  unchecked and block release.

### M8 — Post-v1 complete-data expansion

Depends on M7. This tracks full-data coverage beyond the v1 ordinary-record boundary and does
not block the specifically scoped v1 release. M7 acceptance refers to M0–M7.

- [ ] Review Personal Health Records/FHIR API stability, supported resource types, device
  features, read permissions and distribution requirements for the chosen SDK.
- [ ] Extend the coverage matrix with every supported clinical resource type and its fields,
  source/provenance, paging adapter, access rationale and read-only detail screen.
- [ ] Add progressive clinical-data access and raw resource browsing without interpreting,
  diagnosing, uploading or copying medical records into local persistence.
- [ ] Review ordinary record additions in newer SDKs, including activity intensity, and resolve
  deferred experimental types such as mindfulness with explicit inclusion/deferral decisions.
- [ ] Acceptance: all data available through the adopted public APIs is browsable subject to
  granted access and device support; any remaining limitation is explicitly surfaced and tracked.

## Dependency policy

Every new dependency needs a concrete feature, an APK/build-size review and a check for a small
platform or Kotlin alternative. Prefer AndroidX and Kotlin APIs. Avoid network SDKs, analytics,
crash-reporting uploads, image loaders, reflection-heavy DI and broad utility libraries.

The scaffold includes Activity Compose, Compose UI, Material 3, debug tooling and JUnit. M1 adds
the pinned Health Connect client. M2 adds Lifecycle Compose/ViewModel, Navigation Compose and
Material icons core, plus instrumentation-test dependencies. See the feature/size review in
[the M2 report](docs/m2-verification.md).
Optional local persistence remains deferred until a concrete UI-state requirement appears.

## Definition of done for the scaffold

- [x] `./gradlew :app:assembleDebug` succeeds.
- [x] `./gradlew :app:testDebugUnitTest` succeeds.
- [x] Emulator script unit tests succeed.
- [x] The debug APK installs and launches as `dev.zdrng.yealth/.ui.MainActivity`.
- [x] The screen renders Hello Yealth in light and dark mode.
- [x] API 31+ can use dynamic color and the preview can force the fallback palette.
- [x] Merged manifest contains no Internet or health permission.
- [x] Architecture rules remain executable in the ordinary JVM test suite.

These checks accept the M0 scaffold before product work. Later milestones replace Hello World
and introduce only the read permissions required by their implemented slices.
