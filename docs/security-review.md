# Security review — 24 October 2026

Scope: the 0.2.0 Android source, manifest, build scripts and release packaging. Review performed with Codex (GPT-6 family). This is a source-level review with targeted regression checks, not an independent penetration test.

## Findings addressed

| Area | Change | Reason |
| --- | --- | --- |
| Unused permission | Removed READ_MEDIA_AUDIO | Avoid unnecessary access to the audio library. |
| Picker trust | Resolve and explicitly launch a system/updated-system ringtone picker | Avoid handing SIM labels and selections to an arbitrary third-party picker. |
| Selection trust | Accept only individual local MediaStore audio URIs, with no query or fragment | Reject file/network paths, arbitrary providers and malformed picker results. |
| Target validation | Require a non-null, still-active SIM account before writing | A null account can tell Android to modify the global ringtone. |
| Error recovery | Verify saved values and separately check both rollback operations | Avoid silently claiming recovery when access is revoked or a write fails. |
| Error disclosure | Replace raw exception messages with contextual user messages | Avoid unnecessarily showing framework details or identifiers. |
| Release signing | Require a separate private keystore and explicit release credentials | Prevent accidental public distribution signed with the known development password. |
| Build isolation | Compile into a fresh work directory on each app build | Avoid stale classes entering a release. |

These findings are defensive hardening opportunities. No claim is made that a remotely exploitable vulnerability was demonstrated in the original prototype.

## Checks

Host tests cover 20 accepted/rejected URI cases, including spoofed authorities, file and HTTPS schemes, traversal, encoded paths, query strings and fragments. Manifest checks cover the exact permission set, disabled backup, absence of background/data-sharing components and absence of a debuggable setting. Both the application and instrumentation APK are compiled; the release APK is signature-verified and alignment-checked. Release packaging excludes local signing keys, tool downloads and device artefacts.

The original prototype passed 22 device checks on 18 October 2026, on a Pixel 8 running Android 17 with Backtalk. The owner also confirmed that it worked in use. No phone was connected for fresh device execution of the 0.2.0 hardening changes. Compile success and source review aren't a substitute for testing the new APK on a phone.

## Remaining limitations

WRITE_SETTINGS is a broad Android capability. Android still controls permission enforcement, phone-account data, picker behaviour and ringtone playback. A compromised privileged system picker or framework is outside this app's trust boundary.

The public API depends on Android's per-account ringtone feature flag. Reading the raw `ringtone_id_` setting is an Android 17 implementation detail used for inheritance and rollback; later framework changes may require an app update. Writes are not transactional, so simultaneous changes from another settings app can race with rollback. Avoid changing ringtone settings in multiple apps at once.

The app has no runtime dependencies, internet permission, server, exported service, receiver or provider. The launcher activity doesn't process external extras as commands. Build tools are downloaded from vendor HTTPS endpoints and checked against their metadata; this doesn't provide an independent trust root or a reproducible build guarantee. CI has read-only repository permissions and no release signing secrets.

No critical or high-severity application issue was identified in this scoped review. That conclusion is limited to the checks above and isn't a vulnerability-free certification.

Build note: Google's installed build-tools 37.0.0 D8 emits a warning that its compiler recognises API levels up to 36. It completes DEX generation for min API 37; APK signature and alignment verification pass. This warning is recorded rather than hidden. Device verification of this newly hardened APK remains outstanding.
