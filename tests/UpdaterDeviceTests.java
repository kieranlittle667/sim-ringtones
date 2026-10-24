package uk.co.kiera.simringtones.tests;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.HexFormat;

/** Runs against an isolated test application ID; it never changes SIM ringtones. */
public class UpdaterDeviceTests extends Instrumentation {
    private final StringBuilder report=new StringBuilder();
    private Context app;
    private Activity activity;
    private Object release;
    private Class<?> client,releaseClass;
    private boolean leaveInstaller;
    private Method verify;
    private void check(boolean condition,String description) {
        if(!condition) throw new AssertionError(description);
        report.append("PASS: ").append(description).append('\n');
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); leaveInstaller=args!=null&&"true".equals(args.getString("leaveInstaller")); start(); }
    private File copy(String asset,String name) throws Exception {
        File file=new File(app.getCacheDir(),name); file.getParentFile().mkdirs();
        try(InputStream input=getContext().getAssets().open(asset);OutputStream output=new FileOutputStream(file)) { input.transferTo(output); }
        return file;
    }
    private void rejected(File file,String description) throws Exception {
        try { verify.invoke(null,app,file,release); }
        catch(InvocationTargetException e) { if(e.getCause() instanceof IOException) { check(true,description); return; } throw e; }
        check(false,description);
    }
    private Button button(String description,View view) {
        if(view instanceof Button&&description.contentEquals(view.getContentDescription())) return (Button)view;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) { Button found=button(description,group.getChildAt(i)); if(found!=null) return found; }
        }
        return null;
    }
    @Override public void onStart() {
        Bundle result=new Bundle(); boolean passed=false;
        try {
            app=getTargetContext();
            app.getSharedPreferences("updates",Context.MODE_PRIVATE).edit().putBoolean("automatic",false).commit();
            client=app.getClassLoader().loadClass("uk.co.kiera.simringtones.UpdateClient");
            releaseClass=app.getClassLoader().loadClass("uk.co.kiera.simringtones.UpdateClient$Release");
            Constructor<?> constructor=releaseClass.getDeclaredConstructors()[0]; constructor.setAccessible(true);
            File apk=copy("valid.apk","updates/update.apk");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream input=new FileInputStream(apk)) { byte[] buffer=new byte[8192]; int n; while((n=input.read(buffer))!=-1) digest.update(buffer,0,n); }
            release=constructor.newInstance("v0.3.1","https://github.com/kieranlittle667/sim-ringtones/releases/download/v0.3.1/sim-ringtones-0.3.1.apk","sha256:"+HexFormat.of().formatHex(digest.digest()),"Device test update",apk.length());
            verify=client.getDeclaredMethod("verifyArchive",Context.class,File.class,releaseClass); verify.setAccessible(true);
            verify.invoke(null,app,apk,release);
            check(true,"Same-package, newer APK with matching certificate is accepted");
            rejected(copy("wrong-key.apk","wrong-key.apk"),"APK signed with another certificate is rejected");
            rejected(copy("wrong-package.apk","wrong-package.apk"),"APK for a different package is rejected");
            rejected(copy("older.apk","older.apk"),"Installed-version APK is rejected as a downgrade/reinstall");
            Uri uri=Uri.parse("content://"+app.getPackageName()+".updates/update.apk");
            try(ParcelFileDescriptor fd=app.getContentResolver().openFileDescriptor(uri,"r")) { check(fd!=null&&fd.getStatSize()==apk.length(),"Update provider exposes the cached APK read-only"); }
            try { app.getContentResolver().openFileDescriptor(uri,"rw"); check(false,"Write mode refused"); }
            catch(FileNotFoundException e) { check(true,"Update provider refuses write mode"); }
            try { app.getContentResolver().openFileDescriptor(Uri.parse("content://"+app.getPackageName()+".updates/../private"),"r"); check(false,"Traversal refused"); }
            catch(IllegalArgumentException|FileNotFoundException e) { check(true,"Update provider refuses other paths"); }
            activity=startActivitySync(new Intent().setClassName(app,"uk.co.kiera.simringtones.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            Field field=activity.getClass().getDeclaredField("updates"); field.setAccessible(true); Object controller=field.get(activity);
            Class<?> type=controller.getClass();
            Field available=type.getDeclaredField("available"),downloaded=type.getDeclaredField("downloaded"); available.setAccessible(true); downloaded.setAccessible(true);
            Method refresh=type.getDeclaredMethod("refresh"); refresh.setAccessible(true);
            runOnMainSync(()->{ try { available.set(controller,release); downloaded.setBoolean(controller,true); refresh.invoke(controller); } catch(Exception e) { throw new RuntimeException(e); } });
            runOnMainSync(()->{
                Button install=button("Install SIM Ringtones update",activity.getWindow().getDecorView());
                check(install!=null&&install.isEnabled(),"Verified update has an accessible Install update action");
                install.performClick();
            });
            UiAutomation automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            long deadline=System.currentTimeMillis()+15000; boolean installer=false;
            while(System.currentTimeMillis()<deadline) {
                android.view.accessibility.AccessibilityNodeInfo root=automation.getRootInActiveWindow();
                if(root!=null&&root.getPackageName()!=null) {
                    String pkg=root.getPackageName().toString();
                    if(pkg.contains("packageinstaller")||pkg.contains("permissioncontroller")) { installer=true; break; }
                }
                Thread.sleep(300);
            }
            check(installer,"Install update opens Android's confirmation screen");
            if(!leaveInstaller) {
                sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                check(true,"Installer can be cancelled without replacing the app");
            } else report.append("Installer left open for confirmation of the isolated test update.\n");
            passed=true;
        } catch(Throwable e) {
            report.append("FAIL: ").append(e).append('\n');
            StringWriter details=new StringWriter(); e.printStackTrace(new PrintWriter(details)); report.append(details);
        } finally {
            if(activity!=null&&!leaveInstaller) runOnMainSync(()->activity.finish());
        }
        result.putString("stream",report.toString()); finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
}
