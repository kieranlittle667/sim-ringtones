package uk.co.kiera.simringtones;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

/** Foreground update checks; no service, alarm, notification or silent installation. */
final class UpdateController {
    private static final long DAY=24L*60*60*1000;
    private final Activity activity;
    private final SharedPreferences preferences;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final String currentVersion;
    private TextView status,notes;
    private Button check,action;
    private CheckBox automatic;
    private String message;
    private UpdateClient.Release available;
    private boolean downloaded,busy;
    private volatile boolean closed;

    UpdateController(Activity activity,Bundle state) {
        this.activity=activity;
        preferences=activity.getSharedPreferences("updates",Context.MODE_PRIVATE);
        try { currentVersion=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName; }
        catch(PackageManager.NameNotFoundException e) { throw new IllegalStateException(e); }
        message="Installed version: "+currentVersion;
        if(state==null||!state.containsKey("update_tag")) {
            state=new Bundle();
            state.putString("update_tag",preferences.getString("tag",null));
            state.putString("update_url",preferences.getString("url",null));
            state.putString("update_digest",preferences.getString("digest",null));
            state.putString("update_notes",preferences.getString("notes",""));
            state.putLong("update_size",preferences.getLong("size",0));
            state.putBoolean("update_downloaded",preferences.getString("digest","").equals(preferences.getString("downloaded_digest",null)));
        }
        if(state!=null) {
            String tag=state.getString("update_tag"),url=state.getString("update_url"),digest=state.getString("update_digest");
            long size=state.getLong("update_size");
            if(UpdatePolicy.version(tag)>UpdatePolicy.version(currentVersion)&&UpdatePolicy.downloadUrl(url,tag,"sim-ringtones-"+tag.replaceFirst("^v", "")+".apk")
                    &&UpdatePolicy.digest(digest)&&size>0&&size<=UpdatePolicy.MAX_APK_BYTES) {
                available=new UpdateClient.Release(tag,url,digest,state.getString("update_notes",""),size);
                downloaded=state.getBoolean("update_downloaded")&&cachedApk().isFile();
                message=downloaded?"Update downloaded. Ready to install.":tag+" is available.";
            }
        }
    }
    void saveState(Bundle state) {
        if(available==null) return;
        state.putString("update_tag",available.tag); state.putString("update_url",available.url);
        state.putString("update_digest",available.digest); state.putLong("update_size",available.size);
        state.putString("update_notes",available.notes); state.putBoolean("update_downloaded",downloaded);
    }
    void bind(TextView status,TextView notes,Button check,Button action,CheckBox automatic) {
        this.status=status; this.notes=notes; this.check=check; this.action=action; this.automatic=automatic;
        automatic.setChecked(preferences.getBoolean("automatic",true));
        automatic.setOnCheckedChangeListener((button,enabled)->{
            preferences.edit().putBoolean("automatic",enabled).apply();
            if(enabled) onResume();
        });
        check.setOnClickListener(v->check());
        action.setOnClickListener(v->{ if(downloaded) install(); else download(); });
        refresh();
    }
    void onResume() {
        long now=System.currentTimeMillis(),last=preferences.getLong("last_attempt",0);
        if(!closed&&!busy&&preferences.getBoolean("automatic",true)
                &&(last==0||now<last||now-last>=DAY)) check();
    }
    private void refresh() {
        if(status==null||closed) return;
        status.setText(message);
        check.setEnabled(!busy);
        action.setVisibility(available==null?View.GONE:View.VISIBLE);
        action.setEnabled(!busy);
        action.setText(downloaded?"Install update":"Download update");
        action.setContentDescription(downloaded?"Install SIM Ringtones update":"Download SIM Ringtones update");
        notes.setText(available==null?"":available.notes);
        notes.setVisibility(available==null||available.notes.isEmpty()?View.GONE:View.VISIBLE);
    }
    private void ui(Runnable update) {
        activity.runOnUiThread(()->{ if(!closed&&!activity.isDestroyed()) { update.run(); refresh(); } });
    }
    private void check() {
        if(closed||busy) return;
        busy=true; message="Checking for updates…"; refresh();
        preferences.edit().putLong("last_attempt",System.currentTimeMillis()).apply();
        worker.execute(()->{
            try {
                UpdateClient.Release release=UpdateClient.check(currentVersion);
                ui(()->{
                    boolean same=available!=null&&release!=null&&available.digest.equals(release.digest);
                    available=release; downloaded=downloaded&&same; busy=false;
                    SharedPreferences.Editor saved=preferences.edit();
                    if(release==null) saved.remove("tag").remove("url").remove("digest").remove("notes").remove("size").remove("downloaded_digest");
                    else saved.putString("tag",release.tag).putString("url",release.url).putString("digest",release.digest).putString("notes",release.notes).putLong("size",release.size);
                    saved.apply();
                    message=release==null?"SIM Ringtones is up to date ("+currentVersion+").":release.tag+" is available.";
                });
            } catch(Exception e) {
                ui(()->{ busy=false; message="Could not check for updates. Check your connection and try again."; });
            }
        });
    }
    private File cachedApk() { return new File(activity.getCacheDir(),"updates/update.apk"); }
    private void download() {
        if(closed||busy||available==null) return;
        UpdateClient.Release release=available;
        busy=true; message="Downloading and verifying update…"; refresh();
        worker.execute(()->{
            try {
                UpdateClient.download(activity,release);
                ui(()->{ preferences.edit().putString("downloaded_digest",release.digest).apply(); downloaded=true; busy=false; message="Update downloaded. Ready to install."; });
            } catch(Exception e) {
                ui(()->{ downloaded=false; busy=false; message="Could not verify the update. Nothing was installed. Check your connection or try again later."; });
            }
        });
    }
    private void install() {
        if(closed||busy||available==null||!downloaded) return;
        if(!activity.getPackageManager().canRequestPackageInstalls()) {
            message="Allow SIM Ringtones to install updates, then return and tap Install update."; refresh();
            try { activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()))); }
            catch(RuntimeException e) { message="Open Android settings and allow installing apps from SIM Ringtones, then try again."; refresh(); }
            return;
        }
        UpdateClient.Release release=available;
        busy=true; message="Verifying update before installation…"; refresh();
        worker.execute(()->{
            try {
                // Recheck cached bytes and identity after returning from settings or recreation.
                try(InputStream input=new FileInputStream(cachedApk())) {
                    UpdatePolicy.copyVerified(input,OutputStream.nullOutputStream(),release.size,release.digest);
                }
                UpdateClient.verifyArchive(activity,cachedApk(),release);
                ui(()->{ busy=false; openInstaller(); });
            } catch(Exception e) {
                ui(()->{ preferences.edit().remove("downloaded_digest").apply(); busy=false; downloaded=false; message="The cached update could not be verified. Download it again."; });
            }
        });
    }
    private void openInstaller() {
        // Never open an installer in the background after a download or verification.
        if(!activity.hasWindowFocus()) { message="Update verified. Tap Install update to continue."; return; }
        Uri uri=Uri.parse("content://"+activity.getPackageName()+".updates/update.apk");
        Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            ResolveInfo installer=activity.getPackageManager().resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);
            if(installer==null||installer.activityInfo==null
                    ||(installer.activityInfo.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0) {
                message="No system package installer is available."; return;
            }
            intent.setComponent(new ComponentName(installer.activityInfo.packageName,installer.activityInfo.name));
            activity.startActivity(intent);
            message="Finish the update in Android's installer. If cancelled, tap Install update to try again.";
        } catch(RuntimeException e) { message="Could not open Android's package installer. Nothing was installed."; }
    }
    void close() { closed=true; worker.shutdownNow(); }
}
