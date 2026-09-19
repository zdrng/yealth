# M3 — Access and history verification

Implemented and reviewed on 2026-09-18 using the pinned Nix/JDK 17/SDK 36 environment.

## Delivered behavior

Normal debug and release launches use real Health Connect access. The explicit debug-only
`preview=true` launch retains the M2 fixture flow; release contains no fixture data.

- Access shows granted, partial, missing and not-yet-implemented categories. Only Steps,
  HeartRate and VO₂max have manifest read permissions and visible live raw lists/details.
  Categories explain precisely which implemented types are available before system consent.
- Permission requests use the SDK activity-result contract, remain on the originating route,
  and recheck the provider's actual grants after grant, denial or cancellation. Date, period,
  query, tab and navigation context are preserved.
- Opening Health Connect settings uses the supported SDK settings action. The detail action
  is labeled for settings, without claiming a record-specific destination. Installation/update
  recovery opens the provider's store entry before Android 14 and system settings on newer
  Android. Unresolvable activities show an actionable error instead of crashing.
- SDK availability and feature support are refreshed. Unavailable/update-required providers,
  unsupported features, permission errors, read failures and successful empty queries have
  separate presentations. Planned record types never request permission or run a reader.
- Older-data access has its own explanation and system request. It is feature-gated and never
  bundled into category requests. Denial leaves current type permissions and recent reads usable.
- Both documented system privacy/rationale entry points show the same local policy. No network,
  write or background-read permission exists; backup remains disabled.

## Architecture and lifecycle

`AccessViewModel` and `LiveRecordsViewModel` depend on the framework-free domain service.
`HealthConnectActions` owns SDK permission contracts/constants and Android provider intents;
`AppContainer` wires those into the Activity. No SDK import exists in UI/domain, and no data
implementation imports UI or DI. Existing architecture tests enforce these directions.

Activity stop clears the access environment. Record ViewModels cancel in-flight reads and
pagination, clear values/cursors, and ignore obsolete coroutine results. A new foreground
entry rechecks access before rendering data. A revision check prevents the previous access
snapshot's records from appearing during a new check. Hidden navigation entries do not read.
The metric and its focused detail share the metric entry's in-memory records. Saved state
contains only selections/route IDs, never values or page tokens. Restoring a detail whose
record is not yet loaded offers reload/paging instead of unrelated inactive row actions.

The pinned SDK does not expose the original permission-grant date. Without history access,
queries extending beyond the last 30 days use a visibly labeled conservative recent subset;
the UI shows the actual queried interval and explicitly does not claim this is the permission
boundary. Entirely older selections require history access (or show unsupported history).
Granting history retries the original selected interval. This avoids silently treating an
inaccessible historical interval as an empty result while retaining recent-data browsing.

Raw lists support explicit page loading with ID deduplication. An empty SDK page token is
normalized to completion. Summary aggregation and comprehensive paging/source-data acceptance
remain M4. Live metric chart acceptance remains M6.

## Verification

Commands:

```sh
nix develop -c ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:testDebugUnitTest :app:testReleaseUnitTest :app:lintDebug :app:assembleRelease
```

Use the project's emulator signing environment consistently when installing over `make run`:
`ANDROID_USER_HOME="$HOME/.local/share/yealth/android"`. Do not replace the immutable Nix SDK.

JVM coverage includes category partial grants, planned-type exclusion, denial/cancellation
rechecks, provider recovery/errors, revocation, cancellation of pending reads, no hidden-route
reads, recent-data preservation after history denial, full-range retry after grant, unsupported
history, paging permission races and exact manifest/catalog reconciliation. A repository test
also covers empty continuation tokens.

Device: project API 36 ARM64 Google APIs emulator, 1080×2400. Instrumentation evidence lives in
`scratchpad/m3/`; screenshots contain emulator fixtures or empty provider results, not user data.

| Scenario | Evidence |
| --- | --- |
| All 41 preview routes, detail/back, tab retention and recreation | `regression-results.txt`: five existing tests passed |
| Real category rationale → actual system permission sheet → cancellation | `access-final-results.txt`: selection retained; `m3-system-permissions.png` |
| Real Health Connect settings and return | `access-final-results.txt`, `m3-health-settings.png` |
| Unavailable/update-required provider recovery and unsupported history | Domain/repository tests plus injected repository on the real Compose/device surface |
| Real partial grant (Steps only), separate VO₂max denial | `grant-final-results.txt`, `m3-partial-grant.png` |
| Real history denial preserves recent access, subsequent history grant | `grant-final-results.txt`, `m3-history-system.png`, `m3-recent-after-history-denial.png`, `m3-history-granted.png` |
| Actual permission revocation followed by relaunch and missing-access state | `revocation-light-results.txt`; Steps and history revoked via emulator `pm revoke` |
| Large text permission/access screens | `large-font-results.txt`, `large-font/` screenshots |

The isolated grant-changing device test is opt-in, so a normal navigation run does not grant
access. Run on the dedicated project emulator with Steps/VO₂max/history initially ungranted:

```sh
adb shell am instrument -w \
  -e class dev.zdrng.yealth.HealthAccessDeviceTest#systemPartialGrantDenialAndHistory \
  -e grantFlow true dev.zdrng.yealth.test/androidx.test.runner.AndroidJUnitRunner
```

Restore test permissions afterward with `pm revoke` for READ_STEPS and READ_HEALTH_DATA_HISTORY.
The other system cancellation test assumes Steps is ungranted. The test completes first-run
Health Connect onboarding if needed. Provider absence/update/unsupported-history variants use
injected environments; the framework Health Connect module was not physically removed.

## Scope and remaining work

M3 adds no dependencies. Manifest auditing confirms only READ_STEPS, READ_HEART_RATE,
READ_VO2_MAX and READ_HEALTH_DATA_HISTORY, plus AndroidX's app-local non-exported-receiver
permission. No Internet, write, background read or backup capability is present. R8/resource
shrinking remain enabled and release excludes `ui.preview` and `data.fake` classes.

This milestone establishes working access and raw-record paths. The provider in the emulator
contains no seeded health measurements. Full verification against Health Connect Toolbox source
records, Today/Activity summaries, aggregation, pagination stress and the complete Steps slice
remain M4. Remaining readers are M5; live metric charts are M6. Physical older Android/provider
versions, exhaustive TalkBack/contrast checks and published privacy/Play declarations remain M7.

Repeated Android denials can return without presenting another system dialog. The metric's
missing-access state therefore offers a direct Health Connect settings action as well as
review/retry; device checks cover that return without changing selection or showing zero data.
Display filtering also excludes planned readers that share a permission string with an
implemented reader (for example StepsCadence/Steps). This is covered by a JVM assertion and
an on-device rationale assertion. Final category screenshots are in `partial-updated/`.

Official API references reviewed for the pinned integration:

- [Setup, activity-result permissions and both rationale intents](https://developer.android.com/health-and-fitness/health-connect/get-started)
- [Read restrictions, separate history permission and pagination](https://developer.android.com/health-and-fitness/health-connect/read-data)
- [Provider availability](https://developer.android.com/health-and-fitness/health-connect/availability)

The published connect-client 1.1.0 artifact was inspected for its actual contract/settings
APIs; no newer API described on the rolling documentation site was assumed available.

Final build results: **40 debug JVM tests and 31 release JVM tests passed**. Lint reports
**0 errors, 12 dependency/toolchain update notices**. Debug and R8/resource-shrunk release
builds succeeded. Nine distinct device test methods passed across the ordinary and isolated
system-grant runs; access cases were additionally repeated after revocation, in light theme,
and at 200% font scale. Five M2 navigation regressions include all 41 catalog routes.

| APK | M2 bytes | M3 bytes | Change |
| --- | ---: | ---: | ---: |
| Debug | 36,572,121 | 37,321,904 | +749,783 |
| Unsigned, shrunk release | 2,816,082 | 3,320,870 | +504,788 |

The additional SDK permission flow is now reachable in release and retained by R8. No library
or version-catalog change was needed. Sizes use the project's emulator debug-signing setup.

The public rationale action was launched successfully on the emulator and resolved to
`PrivacyActivity` (`Status: ok`). Both intent filters include DEFAULT for implicit resolution.
The Android 14+ alias retains `START_VIEW_PERMISSION_USAGE` protection: a direct unprivileged
adb launch was correctly rejected by Android. Its target and health category were audited in
the merged manifest. The test emulator's temporary grants and USER_SET/USER_FIXED denial flags
were removed afterward; font scale was reset to 1.0 and normal light-mode launch smoke-tested.
