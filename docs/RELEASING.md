# Signing and releases

Yealth follows Hablock's manually dispatched GitHub Actions release workflow.
It runs JVM tests, lint and emulator-script tests, builds a shrunk release APK,
checks its version and signature, and uploads an artifact. Selecting
`create_release` also creates a `vVERSION` tag and a GitHub Release with generated
notes and the APK. No store upload or automatic release on push is configured.
Device tests remain a local check (`make test-device`).

## One-time key generation

Run from the Yealth project directory on your Mac. Use a **new Yealth key**,
not Hablock's key. These commands prompt for the password and certificate details;
passwords do not appear in command history. Keep the alias `yealth`.

```sh
mkdir -p "$HOME/.local/share/yealth/signing"
chmod 700 "$HOME/.local/share/yealth/signing"
nix develop -c keytool -genkeypair -v \
  -keystore "$HOME/.local/share/yealth/signing/yealth-release.keystore" \
  -storetype PKCS12 -alias yealth -keyalg RSA -keysize 4096 -validity 10000
chmod 600 "$HOME/.local/share/yealth/signing/yealth-release.keystore"
```

Use the same password for `KEYSTORE_PASSWORD` and `KEY_PASSWORD` with this PKCS12
keystore. Generate this key **once**, then reuse it for every update. If the key
already exists, do not replace it or generate another alias for releases.

Inspect and record its SHA-256 certificate fingerprint:

```sh
nix develop -c keytool -list -v \
  -keystore "$HOME/.local/share/yealth/signing/yealth-release.keystore" \
  -alias yealth
```

## Back up before the first release

Keep at least two secure copies of the `.keystore` file, including an encrypted
backup separate from this computer. Save the password, alias (`yealth`), certificate
SHA-256 fingerprint and application ID (`dev.zdrng.yealth`) in your password manager.
Test the backup with the `keytool -list` command using the backup path. Losing the
signing key prevents normal APK updates for existing installations.

GitHub secrets are not a recoverable backup. Base64 is only an encoding: protect
it like the keystore. You do not need to back up build folders or debug keys.
For each published version, retain its APK, version name/code and
`app/build/outputs/mapping/release/mapping.txt` for interpreting crash traces.

## GitHub setup

Push this project, including `.github/workflows/release.yml`, to your GitHub
repository's default branch before running the workflow.

Open **Repository → Settings → Secrets and variables → Actions → Secrets →
New repository secret**. These are repository **secrets**, not repository variables:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of the complete Yealth keystore file |
| `KEYSTORE_PASSWORD` | Password chosen above |
| `KEY_ALIAS` | `yealth` |
| `KEY_PASSWORD` | Same password chosen above |

On macOS, copy the base64 value to your clipboard without printing it:

```sh
base64 -i "$HOME/.local/share/yealth/signing/yealth-release.keystore" | tr -d '\n' | pbcopy
```

Paste it into `KEYSTORE_BASE64`, save, then clear the clipboard (`pbcopy < /dev/null`).
Do not commit a base64 file, keystore or password. GitHub supplies `GITHUB_TOKEN`
automatically; no personal access token is needed. The workflow requests
`contents: write` for the tag and release. Repository rules must permit that.
`KEYSTORE_FILE` is set automatically to a temporary runner path; do not add it as
a GitHub secret. No signing variables belong in `gradle.properties`, `flake.nix`
or the workflow itself.

## Run a release

In **Actions → Release → Run workflow**, select the intended branch/commit:

- `version_name`: e.g. `0.1.0` (no leading `v`).
- `version_code`: e.g. `1`; use a larger integer for every subsequent published
  version, including prereleases. Allowed range: 1–2100000000.
- `create_release`: leave unchecked for the first signed test run. Download the
  artifact and test the APK. Then run the same revision and versions with it checked.

The workflow validates the format and the APK metadata, but does not compare codes
with past releases: you must choose a strictly increasing code. Use a fresh version
name/tag for every new release; existing tags are not overwritten. A run without
any signing secrets may build an unsigned test artifact, but cannot publish it.
Partial or incorrect signing credentials fail the build. Prerelease names do not
automatically mark the GitHub Release as a prerelease (same behavior as Hablock).

A debug-installed app uses a different key, so a release APK cannot update it.
Use a separate test device/profile or remove the debug app first (removal deletes
its local settings). Updates between correctly signed release APKs retain app data.

## Optional local signed build

GitHub secrets are sufficient for hosted releases. For local signing, enter a
Bash session inside the pinned environment and provide variables only for that
session. The password prompt is hidden:

```sh
nix develop -c bash
export KEYSTORE_FILE="$HOME/.local/share/yealth/signing/yealth-release.keystore"
export KEY_ALIAS=yealth
read -r -s -p 'Keystore password: ' KEYSTORE_PASSWORD
printf '\n'
export KEYSTORE_PASSWORD
export KEY_PASSWORD="$KEYSTORE_PASSWORD"
./gradlew :app:assembleRelease -PversionName=0.1.0 -PversionCode=1
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
unset KEYSTORE_FILE KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD
exit
```

Alternatively, like Hablock, create an ignored `keystore.properties` in the project
root with `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. Use an absolute
keystore path (no `$HOME` expansion in properties), restrict it with `chmod 600`,
and escape Java-properties special characters correctly. Relative paths resolve
from the project root. Environment variables override corresponding properties.
The interactive environment approach above avoids properties escaping entirely.
No `.env` file is automatically loaded. Without signing configuration, local
release builds produce `app-release-unsigned.apk`.
