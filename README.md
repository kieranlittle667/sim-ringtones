# SIM Ringtones

I was surprised that choosing a different ringtone for each SIM wasn't a feature exposed on my Pixel. It annoyed me enough that I built this with the help of Codex.

SIM Ringtones is a small Android app that sets a separate ringtone for each active voice SIM. Android stores the settings, so the app doesn't need to keep running.

**Tested on a Pixel 8 running Android 17.** Other phones and Android builds may behave differently.

## Install and use

Download the APK from [Releases](https://github.com/kieranlittle667/sim-ringtones/releases). Android will ask you to allow installation from your browser or file manager. You don't need root, ADB, a Google account or an internet connection to use the app.

1. Open SIM Ringtones and allow SIM access.
2. Open **Allow ringtone changes** and enable Android's **Modify system settings** permission for the app.
3. Choose a ringtone under each SIM using the system sound picker.
4. Call each SIM from another phone to check the result.

Preview plays for up to eight seconds. Tap Preview again to stop it. **Use phone default** removes that SIM's override after confirmation.

Use built-in sounds or local audio added to Android's ringtone library. Arbitrary files, streaming URLs and third-party content-provider selections aren't accepted. Removing a custom audio file later can stop that ringtone working.

## Requirements and limitations

- Android 17 / API 37 or later. The minimum SDK is 37 because this uses Android 17's public per-phone-account ringtone API.
- An Android build with per-phone-account ringtone support enabled (`support_per_phone_account_ringtone`). Android version alone doesn't guarantee that a manufacturer enables it.
- Active voice SIMs. VoIP accounts aren't listed.
- Per-SIM silent mode isn't offered: a null per-account ringtone falls back to the phone's default sound.

The app reads the setting back after saving. If Android unexpectedly changes the global ringtone, it attempts to restore the previous settings and reports the failure. It can't guarantee recovery if Android revokes access during a write. Check your phone's ringtone settings if it reports a restoration problem.

## Accessibility

The interface uses native Android controls, labelled buttons, accessibility headings and spoken status updates. It works with Backtalk and supports enlarged text, including 200% text size.

## Permissions and privacy

`READ_PHONE_STATE` identifies active voice SIMs. `READ_PHONE_NUMBERS` is required by Android's phone-account API even though this app doesn't read or display your phone number. `WRITE_SETTINGS` lets it change ringtone settings and is a broad Android permission; enable it only if you're comfortable granting that access.

There is no internet permission, analytics, advertising, account system or background service. See [Privacy](PRIVACY.md) and [Security](SECURITY.md).

## Build from source

Build instructions and test commands are in [Contributing](CONTRIBUTING.md). The app uses Java and the Android SDK, with no runtime libraries or Gradle requirement.

## Credits and licence

Created by Kieran Little, co-authored with OpenAI Codex. A GPT-6-family model was used for release preparation.

Released under the [MIT licence](LICENSE). Android API reference: [RingtoneManager](https://developer.android.com/reference/android/media/RingtoneManager).
