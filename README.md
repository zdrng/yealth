# Yealth

Offline, read-only Health Connect browser for Android, built with Kotlin, Jetpack Compose
and Material 3 Expressive.

M5 implements all 40 stable ordinary Health Connect readers, progressive category access,
paged raw records and nested details. Today displays provider Steps/Sleep/Heart summaries;
Steps Week includes seven exact daily provider totals in an expressive rounded-bar chart.
Reusable Material 3 Expressive components follow the supplied design references.
Mindfulness remains an explicit experimental deferral; clinical/FHIR records remain post-v1.
See [M5 implementation and verification](docs/m5-verification.md) and
[design decisions](docs/m5-design.md). The next milestone is M6 specialized metric histories.

Normal debug and release launches use Health Connect. For the isolated debug sample flow:
`adb shell am start -n dev.zdrng.yealth/.ui.MainActivity --ez preview true`.
Release never includes sample records. No network, write or background-read permissions exist.

The product goal is comprehensive browsing of all available Health Connect data. See the
[coverage matrix](docs/health-connect-coverage.md) for implemented readers and explicit limits.

## Quick start

With Nix and flakes enabled:

```sh
make build
make test
make run
```

`make help` lists emulator, screenshot, log and test commands. See [PLAN.md](PLAN.md) for
the product scope, architecture and implementation roadmap.

## Releases

The manual GitHub Actions workflow tests, builds, verifies and optionally publishes
a signed APK, following Hablock’s release process. See [signing and releases](docs/RELEASING.md)
for key generation, backups, GitHub secrets and local builds.
