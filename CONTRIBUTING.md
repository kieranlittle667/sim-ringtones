# Contributing

Keep changes small and explain the behaviour they fix. Native controls, screen-reader labels, large touch targets and enlarged text support are part of the app's requirements.

Run `setup-tools.ps1`, `tests/run-tests.ps1` and `build.ps1` on Windows. The host tests exercise URI trust boundaries and check the manifest's permission/component contract. CI builds a development APK; official releases are signed separately.

## Device checks

Use a test phone with Android 17, two active voice SIMs and per-account ringtone support. Record the existing global and SIM settings first. The instrumentation test changes ringtone settings temporarily and attempts to restore them in `finally`; don't run it during an important call or on a phone whose settings you can't restore manually.

Build the development app first, then run `tests/build-tests.ps1`. With ADB and USB debugging enabled:

```text
adb install -r build/sim-ringtones.apk
adb shell pm grant uk.co.kiera.simringtones android.permission.READ_PHONE_STATE
adb shell pm grant uk.co.kiera.simringtones android.permission.READ_PHONE_NUMBERS
adb shell cmd appops set uk.co.kiera.simringtones WRITE_SETTINGS allow
adb install -r build/tests/tests.apk
adb shell am instrument -w uk.co.kiera.simringtones.tests/.DeviceTests
```

The instrumentation APK and target app must share the development signing key. It cannot instrument the official release APK. Don't uninstall an installed app merely to run tests without first understanding the signing difference.

Also test denied/revoked permissions, picker cancellation, a removed or disabled SIM, built-in and local custom tones, reset, screen-reader navigation and 200% text. Place an incoming call to each SIM and confirm the actual sound. Test preview stopping when the app is backgrounded.

Never commit `.tools`, `build`, private signing material, device logs or unredacted screenshots. Include only the minimum information needed in an issue. Security reports belong in the private reporting channel described in SECURITY.md.

## Build environment and release signing

Use 64-bit Windows, PowerShell 5.1 or 7, an internet connection for downloads and Windows' `tar` command. Review the Android SDK licence terms before using the tools. `setup-tools.ps1` downloads Temurin JDK 21 and Google's API 37 platform/build tools, and checks archive hashes against vendor HTTPS metadata.

`build.ps1` writes `build/sim-ringtones.apk`, signed with a locally generated development key. For a release, set `SIM_RINGTONES_KEYSTORE` to a private PKCS12 keystore and `SIM_RINGTONES_STORE_PASSWORD` to its password, then run `./build.ps1 -Release`. The alias defaults to `simringtones`; use `-KeyAlias` to change it. Use the same password for the key and store. Clear the environment variable after building and back up the key securely. Release builds don't fall back to a development key.

Official release APKs include a SHA-256 checksum and certificate fingerprint. Vendor tool downloads can change over time; identical APK hashes across rebuilds aren't guaranteed.

## Updater tests

Host tests include release metadata fixtures, version ordering, trusted download URLs, SHA-256 and size failures, cancellation and manifest permission checks. `tests/setup-test-tools.ps1` downloads a hash-pinned `org.json` JAR for host tests only. It is not bundled in the app; Android supplies its runtime JSON parser.

`tests/build-updater-device-tests.ps1` creates an isolated app ID, `uk.co.kiera.simringtones.updatertest`, and APK fixtures with valid, wrong-key, wrong-package and older versions. It writes paths to `build/updater-device-paths.json`. Install those app/test APKs on a spare Android 17 phone, allow the isolated app to install packages and run `adb shell am instrument -w uk.co.kiera.simringtones.updatertest.tests/.UpdaterDeviceTests`. It exercises archive rejection, read-only file sharing and opening/cancelling Android's installer. It does not change SIM ringtones or replace the regular app. Keep the phone unlocked during the installer check. Remove both isolated test packages afterwards.

For release testing, also check automatic checks on/off, manual checking offline, a real newer release, a cancelled download, rotation, returning from installation permission settings, installer cancellation and a completed installation. Use two consecutively versioned APKs signed with the same test key; never publish test fixtures as stable releases.

Stable releases need a `vMAJOR.MINOR.PATCH` tag and exactly one asset named `sim-ringtones-MAJOR.MINOR.PATCH.apk`, uploaded normally so GitHub supplies its SHA-256 digest. Increase both Android version code and semantic version. Drafts and prereleases are ignored. This self-updater is intended for GitHub APK distribution; assess store policies separately before distributing through an app store.
