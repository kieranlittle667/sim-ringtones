# Security

Security fixes apply to the latest 0.2.x release. Earlier personal test builds aren't supported for public distribution.

Please report vulnerabilities privately using this repository's **Security → Report a vulnerability** option. Include the Android build, app version, impact and steps to reproduce. Don't put personal SIM information, device serial numbers, private keys or full device logs in a public issue. If private reporting is unavailable, open an issue asking for a private reporting channel without disclosing the vulnerability.

The app needs Android's broad Modify system settings permission. Its intended writes are limited to ringtone settings. It accepts local MediaStore audio selections from the system ringtone picker, validates that the target is still an active SIM, and reads settings back after saving. There is no network service or exported data provider.

The source-level review and its limits are recorded in [the release security review](docs/security-review.md). This is not an independent penetration test or a guarantee that the app or Android framework has no vulnerabilities.

Release APKs use a private release signing key. Development keys, local toolchains and device artefacts are excluded from Git. Compare release checksums and certificate fingerprints when verifying a download. A checksum in the same release detects corruption; it isn't independent proof of authenticity.
