# Changelog

## 0.3.1 — 4 November 2026

A maintenance release that updates the Android 17 build tools and strengthens release checks.

- Use Google's checksum-verified D8 9.5.29 compiler, which recognises Android 17 / API 37 and resolves the unsupported-API warning from the previous compiler.
- Treat D8 compiler warnings as build failures, and build the isolated updater test app and APK fixtures in Windows CI.
- Generate updater test versions from the app manifest so newer-version checks remain valid when the release version changes.
- Update the commit-pinned GitHub checkout and artifact upload actions to 7.0.1 and 7.0.2.

The app continues to require Android 17 / API 37 and a phone with per-account ringtone support enabled. This release contains no new ringtone controls or changes to the in-app update workflow.

## 0.3.0 — 24 October 2026

- Check GitHub for updates on opening the app, with a daily limit and an off switch.
- Download and verify stable releases, then hand installation to Android for confirmation.
- Show release notes and accessible update status in the app.


## 0.2.0 — 24 October 2026

First public release, based on the Pixel 8 prototype.

- Set, preview and reset a ringtone for each active voice SIM on supported Android 17 builds.
- Native accessible interface with enlarged text support.
- Removed the unused audio-library permission.
- Restricted picker launches to system applications and accepted selections to local MediaStore audio URIs.
- Recheck the active SIM before writing; reject null or stale accounts.
- Check saved settings and report rollback failures without exposing raw exception details.
- Added host regression tests, build CI, public documentation and separate release signing.

## 0.1 — 18 October 2026

Personal prototype tested on a Pixel 8 running Android 17. Not a public release.
