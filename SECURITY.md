# Security

Security fixes apply to the latest public release. Unreleased changes are reviewed through pull requests.

Please report vulnerabilities privately using this repository's **Security → Report a vulnerability** option. Include the Android build, app version, impact and steps to reproduce. Don't put personal SIM information, device serial numbers, private keys or full device logs in a public issue. If private reporting is unavailable, open an issue asking for a private reporting channel without disclosing the vulnerability.

The app needs Android's broad Modify system settings permission. Its intended writes are limited to ringtone settings. It accepts local MediaStore audio selections from the system ringtone picker, validates that the target is still an active SIM, and reads settings back after saving. There is no listening network service or exported data provider. The update provider is private; Android's installer receives temporary read access to one cached APK URI.

The source-level review and its limits are recorded in [the release security review](docs/security-review.md). This is not an independent penetration test or a guarantee that the app or Android framework has no vulnerabilities.

Release APKs use a private release signing key. Development keys, local toolchains and device artefacts are excluded from Git. Compare release checksums and certificate fingerprints when verifying a download. A checksum in the same release detects corruption; it isn't independent proof of authenticity.

## Updater

The [updater review](docs/updater-security-review.md) records its trust boundaries and tests.

Updates come only from stable releases of `kieranlittle667/sim-ringtones` on GitHub. The updater restricts initial APK URLs to that repository and follows HTTPS redirects only to named GitHub download hosts. Metadata and APK size limits, timeouts, SHA-256 verification and archive checks run before handing a file to Android's installer.

The APK must have this app's package name, a newer Android version code, a version name matching the release tag, a compatible minimum SDK and the same current signing certificate. Missing certificates and key changes are rejected. Android independently verifies the signature during installation. Every install requires a user action and Android confirmation. A development-signed installation cannot accept the official release key through this updater.

The updater trusts GitHub's API and infrastructure to describe releases and the private release signing key to authenticate APKs. SHA-256 alone is not publisher authentication. Signing-key rotation would require a deliberate updater change; it is currently refused.
