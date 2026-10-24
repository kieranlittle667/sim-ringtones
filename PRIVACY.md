# Privacy

SIM Ringtones runs locally. It has no internet permission, sends nothing to a server and includes no analytics, advertising or crash-reporting SDK.

It reads active phone-account identifiers and SIM labels to show the lines available on your device. Although Android requires the phone-numbers permission for the phone-account API, the app does not request or display the number itself. It reads ringtone titles and URIs to display, preview and save your selections.

Ringtones are stored in Android system settings, rather than an app database. Picker state may be saved temporarily by Android when the activity is recreated. Android app backup is disabled. The app doesn't write its own diagnostic logs containing these values; Android and its sound picker have their own behaviour and permissions.

Uninstalling the app revokes its access. You can also revoke SIM access and Modify system settings in Android settings. To remove a SIM override, use **Use phone default** before uninstalling.

Bug reports are public unless submitted privately. Don't attach phone numbers, account identifiers, full device logs or screenshots containing personal SIM labels.
