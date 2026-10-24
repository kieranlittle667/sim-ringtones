# Updater review — 24 October 2026

Scope: the proposed 0.3.0 updater. The existing ringtone implementation is unchanged apart from adding update controls to its activity.

## Trust boundaries

- The release API is fixed to `https://api.github.com/repos/kieranlittle667/sim-ringtones/releases/latest`.
- Only stable semantic-version tags newer than the installed version are offered. The expected asset name includes that version; duplicate or missing matching assets are refused.
- APK URLs must point to this repository's release-download path. Redirects are manually followed, with a limit, only over HTTPS to `github.com`, `api.github.com`, `release-assets.githubusercontent.com` or `objects.githubusercontent.com`. User information, nonstandard ports, unknown hosts and fragments are refused.
- Metadata is bounded to 1 MiB and APKs to 50 MiB, with connection/read timeouts. The download must match both GitHub's asset size and SHA-256 digest.
- Android must read the archive as the same package, with a higher version code, matching release version, supported minimum SDK and exactly the same current signing certificate. Unknown certificates and key changes fail closed. Android performs its own verification when installing.
- The cached APK is rechecked before opening the installer. Its provider is not exported; only `/update.apk` can receive a temporary URI grant, and only read access is implemented. Other paths and writes are rejected.
- Checks run off the UI thread. Automatic checks happen on opening the app, at most daily, and have an off switch. There is no scheduled background service, silent download or silent installation.
- The user requests the download and separately requests installation. Only a system installer is launched. The app does not launch an installer after losing window focus.

## Verification

85 host cases passed: 20 ringtone URI checks, 45 updater policy cases and 20 release-metadata cases. Manifest checks cover permissions, disabled backup and cleartext traffic, no background components, and the private single-path provider. Both the app and original instrumentation tests compile.

A live GitHub check parsed the current public release and followed its APK download redirects; size and SHA-256 validation passed.

An isolated test app on a Pixel 8 running Android 17 passed 10 device checks: matching-certificate acceptance; rejection of another certificate, another package and an old version; read-only APK access and refusal of writes/other paths; an accessible install action; opening Android's confirmation screen; and installer cancellation. The test app used its own application ID and did not change SIM ringtone settings. Temporary test packages were removed afterwards.

The final confirmation of a completed installation through the UI remains a release test. Opening and cancelling the installer is verified; it does not establish that the complete update workflow has finished. Tests don't cover every manufacturer's installer or every network condition.

## Remaining risks and deliberate limits

GitHub's API, HTTPS infrastructure and the private release signing key are trust dependencies. The digest is an integrity check, not independent proof of publisher identity. Compromise of the signing key is outside what this client can prevent. Key rotation requires a deliberate implementation change.

A cached update can be removed by Android; the user must download it again. The app may have to repeat a download after its process is killed or a configuration change interrupts it. Automatic checks do not run while the app remains closed. A network failure does not affect ringtone settings.

This is a source review with targeted tests, not an independent penetration test. No critical or high-severity issue was identified within this scope.

References: [GitHub release asset digests](https://github.blog/changelog/2025-06-03-releases-now-expose-digests-for-release-assets/), [GitHub releases API](https://docs.github.com/en/rest/releases/releases), [Android URI grants](https://developer.android.com/guide/topics/manifest/grant-uri-permission-element), [Android installation permission](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()).
