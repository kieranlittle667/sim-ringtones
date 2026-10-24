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
