# M0 / M1 verification — 2026-09-18

M0 is accepted and M1 is implemented. Product UI remains the Expressive scaffold; M2 implements
navigation/screens, M3 requests access, and M4–M6 connect and complete the visible data slices.

## Implemented boundary

- Pinned `androidx.health.connect:connect-client:1.1.0`, the stable release listed in the
  [official release notes](https://developer.android.com/jetpack/androidx/releases/health-connect).
- Catalogued all 41 ordinary `Record` types from that artifact with stable IDs, categories,
  exact read permissions, time semantics, top-level field schemas and feature gates.
- Implemented representative Steps (interval), VO₂max (instant) and HeartRate (series) readers
  using the same typed adapter/pagination boundary. All remaining readers are explicitly
  planned, or marked experimental review; none return invented empty data.
- Added pure Kotlin models, repository/service contracts, query-bound page cursors, source
  metadata and raw values/samples. Formatting remains a UI responsibility.
- Added provider availability, feature, permission, history and failure states; cancellation
  is propagated and exception messages are not forwarded as health data or logged.
- Wired the lazy production graph through `AppContainer`. Creating the application does not
  query health records or request permissions. The fake repository exists only in `src/debug`.
- Added an SDK inventory/field-coverage test and expanded architecture checks. Production code
  uses explicit typed registrations; SDK reflection is confined to JVM coverage tests.

## Acceptance evidence

All commands ran through the repository's `nix develop -c` environment on Apple Silicon macOS.

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug` | Passed. |
| `./gradlew :app:testDebugUnitTest` | 23 tests, zero failures/errors. |
| `./gradlew :app:testReleaseUnitTest` | 14 tests, zero failures/errors; nine service tests using the debug-only fake run in the debug suite. |
| `python3 scripts/test_emulator.py` | Eight tests passed. |
| `./gradlew :app:assembleRelease` | Passed with R8 and resource shrinking. |
| `./gradlew :app:lintDebug` | Zero errors; seven notices about newer versions of already-pinned scaffold dependencies/tools. No new source-code lint findings. |
| `scripts/emulator.sh up` | API 36 Google APIs ARM64, `yealth-api36-arm64-v8a`, `emulator-5554`: install succeeded, launch returned `Status: ok`, process remained running. |
| Light/dark visual inspection | Hello Yealth and subtitle visible without clipping in both modes. Local captures: `scratchpad/m0/light.png` and `scratchpad/m0/dark.png`. Emulator stopped after verification. |
| Theme behavior | API 36 main screen uses the dynamic-color path. Source-checked `HelloYealthPreview` forces `dynamicColor = false`; static schemes exist for both modes. Older API/device matrix remains M7. |
| Debug/release merged manifests | No Internet or health read/write permission; backups disabled. Only the AndroidX-generated signature receiver permission is present. Health Connect package visibility query added. |
| Dependency audit | Health Connect adds its local SDK/proto/external-protobuf artifacts and AndroidX support dependencies; no HTTP client, analytics or upload SDK. Captured graph: `scratchpad/m1/release-dependencies.txt`. |

The permission/provider tests use a deterministic SDK gateway. Actual Health Connect data reads,
system permission dialogs, route consent and Toolbox fixture comparison remain device acceptance
work in M3–M6; the emulator smoke test does not claim to verify those flows.

## Dependency and size review

The Jetpack client is needed for the typed read API and compatibility across the supported Android
range. Using platform-only APIs would not provide the pre-Android-14 integration required by this
project. No DI framework, database, network client or extra test library was added.

| Artifact | Existing scaffold artifact before M1 | M1 build | Increase |
| --- | ---: | ---: | ---: |
| Debug APK | 29,466,643 bytes | 34,846,099 bytes | 5,379,456 bytes |
| Unsigned shrunk release APK | 1,357,649 bytes | 1,395,687 bytes | 38,038 bytes |

These are local APK measurements, not download sizes. The release delta is small at this milestone
because product UI does not yet invoke the complete read graph and R8 can remove unused paths.
Re-measure as each visible reader is connected; do not extrapolate this size to the finished app.

## Scope

The complete-data requirement is now explicit in `PLAN.md`, with a per-type permission/reader/UI
matrix in `health-connect-coverage.md`. Clinical/FHIR browsing is tracked as post-v1 M8, while
ordinary stable health/fitness coverage is a v1 acceptance requirement. Mindfulness is explicitly
experimental in the pinned artifact; activity intensity requires a later SDK review.
