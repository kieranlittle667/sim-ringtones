# Changelog

## 0.3.0 � 24 October 2026

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
