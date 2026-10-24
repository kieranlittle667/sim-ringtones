# SIM Ringtones

I was surprised that choosing a different ringtone for each SIM wasn't a feature exposed on my Pixel. It annoyed me enough that I built this with the help of Codex.

SIM Ringtones is a small Android app that sets a separate ringtone for each active voice SIM. Android stores the settings, so the app doesn't need to keep running.

**Tested on a Pixel 8 running Android 17.** I tested the original app on my phone and confirmed it worked. Other phones and Android builds may behave differently.

## Install and use

Download the APK from [Releases](https://github.com/kieranlittle667/sim-ringtones/releases). Android will ask you to allow installation from your browser or file manager. You don't need root, ADB, a Google account or an internet connection to use the app.

1. Open SIM Ringtones and allow SIM access.
2. Open **Allow ringtone changes** and enable Android's **Modify system settings** permission for the app.
3. Choose a ringtone under each SIM using the system sound picker.
4. Call each SIM from another phone to check the result.

Preview plays for up to eight seconds. Tap Preview again to stop it. **Use phone default** removes that SIM's override after confirmation.

Use built-in sounds or local audio added to Android's ringtone library. Arbitrary files, streaming URLs and third-party content-provider selections aren't accepted. Removing a custom audio file later can stop that ringtone working.

If you installed the earlier personal test APK, uninstall it before installing the first public release: they have different signing certificates. You'll need to grant permissions again. Future public APK updates use the release signing key.

## Requirements and limitations

- Android 17 / API 37 or later. The minimum SDK is 37 because this uses Android 17's public per-phone-account ringtone API.
- An Android build with per-phone-account ringtone support enabled (`support_per_phone_account_ringtone`). Android version alone doesn't guarantee that a manufacturer enables it.
- Active voice SIMs. VoIP accounts aren't listed.
- Per-SIM silent mode isn't offered: a null per-account ringtone falls back to the phone's default sound.

The app reads the setting back after saving. If Android unexpectedly changes the global ringtone, it attempts to restore the previous settings and reports the failure. It can't guarantee recovery if Android revokes access during a write. Check your phone's ringtone settings if it reports a restoration problem.

## Accessibility and testing

The interface uses native Android controls, labelled buttons, accessibility headings, announced status updates and a scrolling layout. Controls are at least 48 dp high.

The original prototype passed 22 device checks on the Pixel 8 on 18 October 2026, with Backtalk enabled. These covered two independent SIM settings, preserving the global ringtone, resetting to the phone default, picker cancellation, button labels and sizes, and text at 200%. The layout was also visually checked at normal and enlarged text sizes. I separately confirmed the app worked in use.

The public 0.2.0 build adds security hardening. Its build, signing and host-side regression checks are recorded in the [security review](docs/security-review.md). Those changes haven't had a fresh device test in this release session. Automated setting checks don't simulate incoming calls; please test both SIMs after installing, particularly on a different phone.

## Permissions and privacy

`READ_PHONE_STATE` identifies active voice SIMs. `READ_PHONE_NUMBERS` is required by Android's phone-account API even though this app doesn't read or display your phone number. `WRITE_SETTINGS` lets it change ringtone settings and is a broad Android permission; enable it only if you're comfortable granting that access.

There is no internet permission, analytics, advertising, account system or background service. See [Privacy](PRIVACY.md) and [Security](SECURITY.md).

## Build from source

Use 64-bit Windows, PowerShell 5.1 or 7, an internet connection for the tool download, and Windows' `tar` command. Review Android's SDK licence terms before using its tools. From the repository folder:

```powershell
./setup-tools.ps1
./tests/run-tests.ps1
./build.ps1
```

The setup script downloads Temurin JDK 21 and Google's Android API 37 platform/build tools. It verifies archive checksums against the vendors' HTTPS metadata. No Gradle installation or runtime libraries are required. The build compiles Java 17 source and writes `build/sim-ringtones.apk`. This is a development build signed with a locally generated test key; don't distribute it as an official release.

For a release, create and securely back up your own private signing keystore. Set `SIM_RINGTONES_KEYSTORE` to its path and `SIM_RINGTONES_STORE_PASSWORD` to its password, then run `./build.ps1 -Release`. The alias defaults to `simringtones` and can be changed with `-KeyAlias`. Both key and store use the same password. Release builds refuse to fall back to the development key. Clear the password environment variable afterwards. Never commit signing keys or passwords.

See [Contributing](CONTRIBUTING.md) for device tests. Build tool downloads aren't pinned byte-for-byte across time, so the project doesn't claim reproducible APK hashes. Official release APKs have a SHA-256 checksum and a signing certificate fingerprint in their release notes.

## Credits and licence

Created by Kieran Little, co-authored with OpenAI Codex. Codex assisted with implementation, accessibility checks, documentation, security review and release preparation. This release was prepared with a GPT-6-family Codex agent. The exact model identifier used for the original prototype wasn't recorded, so I can't reliably name it.

Released under the [MIT licence](LICENSE). Android API reference: [RingtoneManager](https://developer.android.com/reference/android/media/RingtoneManager).
