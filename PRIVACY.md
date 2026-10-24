# Privacy

Ringtone settings and previews run locally. SIM Ringtones includes no analytics, advertising or crash-reporting SDK.

The updater contacts GitHub to check this project's latest stable release and download an update you request. GitHub and its download servers receive your IP address and ordinary connection information, plus the app's updater User-Agent. SIM labels, phone numbers, ringtone selections and device identifiers are not sent. Automatic checks run when you open the app, at most once a day, and can be disabled. Manual checks still contact GitHub when automatic checks are off.

The app stores the update preference, last-check time and available-release information in private preferences. A requested APK download is kept in private cache until replaced or cleared by Android. Only the cached APK can be shared, read-only, with Android's installer when you choose to install it. The installer checks the update and asks for confirmation. There is no silent installation or background update service.

It reads active phone-account identifiers and SIM labels to show the lines available on your device. Although Android requires the phone-numbers permission for the phone-account API, the app does not request or display the number itself. It reads ringtone titles and URIs to display, preview and save your selections.

Ringtones are stored in Android system settings, rather than an app database. Picker state may be saved temporarily by Android when the activity is recreated. Android app backup is disabled. The app doesn't write its own diagnostic logs containing these values; Android and its sound picker have their own behaviour and permissions.

Uninstalling the app revokes its access. You can also revoke SIM access and Modify system settings in Android settings. To remove a SIM override, use **Use phone default** before uninstalling.

Bug reports are public unless submitted privately. Don't attach phone numbers, account identifiers, full device logs or screenshots containing personal SIM labels.
