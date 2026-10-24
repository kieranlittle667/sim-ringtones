package uk.co.kiera.simringtones.tests;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.*;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.*;
import android.telecom.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

public class DeviceTests extends Instrumentation {
    private final StringBuilder report=new StringBuilder();
    private final Map<PhoneAccountHandle,String> originals=new LinkedHashMap<>();
    private Context context;
    private Activity activity;
    private String global;
    private int buttons,headings;
    private Configuration originalConfig;
    private boolean automaticOriginally;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private void check(boolean pass,String description) {
        if(!pass) throw new AssertionError(description);
        report.append("PASS: ").append(description).append('\n');
    }
    private String raw(PhoneAccountHandle h) { return Settings.System.getString(context.getContentResolver(),"ringtone_id_"+h.getId()); }
    private String norm(Uri uri) { return uri==null?null:uri.toString().replace("content://0@","content://"); }
    @Override public void onStart() {
        Bundle result=new Bundle(); boolean passed=false;
        try {
            context=getTargetContext();
            // Do not contact GitHub as a side effect of the ringtone UI tests.
            automaticOriginally=context.getSharedPreferences("updates",Context.MODE_PRIVATE).getBoolean("automatic",true);
            context.getSharedPreferences("updates",Context.MODE_PRIVATE).edit().putBoolean("automatic",false).commit();
            global=Settings.System.getString(context.getContentResolver(),Settings.System.RINGTONE);
            TelecomManager telecom=context.getSystemService(TelecomManager.class);
            for(PhoneAccountHandle h:telecom.getCallCapablePhoneAccounts()) {
                PhoneAccount a=telecom.getPhoneAccount(h);
                if(a!=null&&a.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) originals.put(h,raw(h));
            }
            check(originals.size()==2,"Two active voice SIMs detected");
            ArrayList<Uri> tones=new ArrayList<>();
            try(Cursor c=context.getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                    new String[]{"_id"},"is_ringtone=1",null,"_id")) {
                while(c!=null&&c.moveToNext()&&tones.size()<2) tones.add(ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,c.getLong(0)));
            }
            check(tones.size()==2,"Two built-in test sounds available");
            activity=startActivitySync(new Intent().setClassName(context,"uk.co.kiera.simringtones.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            Method save=activity.getClass().getDeclaredMethod("save",PhoneAccountHandle.class,String.class,Uri.class);
            save.setAccessible(true);
            // Rejected selections and accounts must not change any ringtone.
            PhoneAccountHandle first=originals.keySet().iterator().next();
            for(Uri invalid:new Uri[]{Uri.parse("file:///sdcard/tone.mp3"),Uri.parse("https://example.com/tone.mp3"),Uri.parse("content://untrusted/audio/1")}) {
                runOnMainSync(()->{try { save.invoke(activity,first,"Rejected test",invalid); } catch(Exception e) { throw new RuntimeException(e); }});
                for(PhoneAccountHandle h:originals.keySet()) check(Objects.equals(originals.get(h),raw(h)),"Rejected URI leaves SIM settings unchanged");
                check(Objects.equals(global,Settings.System.getString(context.getContentResolver(),Settings.System.RINGTONE)),"Rejected URI leaves global ringtone unchanged");
            }
            runOnMainSync(()->{try { save.invoke(activity,null,"Invalid account",tones.get(0)); } catch(Exception e) { throw new RuntimeException(e); }});
            check(Objects.equals(global,Settings.System.getString(context.getContentResolver(),Settings.System.RINGTONE)),"Null account cannot change global ringtone");
            int index=0;
            Map<PhoneAccountHandle,String> expected=new LinkedHashMap<>(originals);
            for(PhoneAccountHandle h:originals.keySet()) {
                final Uri tone=tones.get(index++);
                runOnMainSync(()->{try { save.invoke(activity,h,"Device test SIM",tone); } catch(Exception e) { throw new RuntimeException(e); }});
                check(Objects.equals(norm(RingtoneManager.getRingtoneUriForPhoneAccountHandle(context,h)),norm(tone)),"App saves and reads back SIM "+index+" ringtone");
                expected.put(h,raw(h));
                for(PhoneAccountHandle other:originals.keySet()) check(Objects.equals(raw(other),expected.get(other)),"Other SIM setting remains independent");
                check(Objects.equals(global,Settings.System.getString(context.getContentResolver(),Settings.System.RINGTONE)),"Main phone ringtone unchanged");
            }
            for(PhoneAccountHandle h:originals.keySet()) {
                runOnMainSync(()->{try { save.invoke(activity,h,"Device test SIM",null); } catch(Exception e) { throw new RuntimeException(e); }});
                check(raw(h)==null,"Use phone default clears the per-SIM override");
                check(Objects.equals(norm(RingtoneManager.getRingtoneUriForPhoneAccountHandle(context,h)),norm(RingtoneManager.getActualDefaultRingtoneUri(context,1))),"Reset follows the current phone ringtone");
            }
            runOnMainSync(()->{
                buttons=0;headings=0;
                inspect(activity.getWindow().getDecorView());
            });
            check(buttons==9,"Nine labelled visible native buttons with at least 48dp touch targets");
            check(headings==4,"App title, SIM names and Updates are accessibility headings");
            check(Settings.System.canWrite(context),"Required system settings access granted");
            snapshot("layout-normal.png");
            originalConfig=new Configuration(activity.getResources().getConfiguration());
            runOnMainSync(()->{
                Configuration large=new Configuration(originalConfig); large.fontScale=2f;
                activity.getResources().updateConfiguration(large,activity.getResources().getDisplayMetrics());
                render();
            });
            waitForIdleSync();
            // Await a real layout traversal before measuring and drawing.
            runOnMainSync(()->activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() { activity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(this); }
            }));
            waitForIdleSync();
            runOnMainSync(()->checkNoClipping(activity.getWindow().getDecorView()));
            snapshot("layout-large-text.png");
            check(true,"200 percent text renders without truncated labels");
            runOnMainSync(()->{
                activity.getResources().updateConfiguration(originalConfig,activity.getResources().getDisplayMetrics());
                render();
                try {
                    Method choose=activity.getClass().getDeclaredMethod("choose",PhoneAccountHandle.class,String.class);
                    choose.setAccessible(true); choose.invoke(activity,originals.keySet().iterator().next(),"SIM 1");
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            UiAutomation automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            try { automation.waitForIdle(500,5000); } catch(java.util.concurrent.TimeoutException ignored) { }
            android.view.accessibility.AccessibilityNodeInfo root=automation.getRootInActiveWindow();
            check(root!=null&&"com.google.android.soundpicker".contentEquals(root.getPackageName()),"Choose ringtone opens Pixel's native sound picker");
            Bitmap picker=automation.takeScreenshot();
            if(picker!=null) {
                try(FileOutputStream file=new FileOutputStream(new File(context.getExternalFilesDir(null),"picker.png"))) { picker.compress(Bitmap.CompressFormat.PNG,100,file); }
                picker.recycle();
            }
            sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            try { automation.waitForIdle(500,5000); } catch(java.util.concurrent.TimeoutException ignored) { }
            check(originals.keySet().stream().allMatch(h->raw(h)==null),"Cancelling the picker leaves both SIM ringtones unchanged");
            passed=true;
        } catch(Throwable e) {
            report.append("FAIL: ").append(e).append('\n');
            StringWriter sw=new StringWriter(); e.printStackTrace(new PrintWriter(sw)); report.append(sw);
        } finally {
            try {
                for(Map.Entry<PhoneAccountHandle,String> entry:originals.entrySet()) {
                    String value=entry.getValue(); RingtoneManager.setRingtoneUri(context,value==null?null:Uri.parse(value),entry.getKey());
                    if(!Objects.equals(raw(entry.getKey()),value)) Settings.System.putString(context.getContentResolver(),"ringtone_id_"+entry.getKey().getId(),value);
                }
                check(originals.entrySet().stream().allMatch(e->Objects.equals(e.getValue(),raw(e.getKey()))),"All original SIM ringtone settings restored");
                check(Objects.equals(global,Settings.System.getString(context.getContentResolver(),Settings.System.RINGTONE)),"Original main ringtone preserved after tests");
                if(activity!=null) runOnMainSync(()->{
                    if(originalConfig!=null) activity.getResources().updateConfiguration(originalConfig,activity.getResources().getDisplayMetrics());
                    render();
                });
            } catch(Throwable e) { passed=false;report.append("RESTORE FAILURE: ").append(e).append('\n'); }
        }
        if(context!=null) context.getSharedPreferences("updates",Context.MODE_PRIVATE).edit().putBoolean("automatic",automaticOriginally).commit();
        result.putString("stream",report.toString()); finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private void render() {
        try { Method method=activity.getClass().getDeclaredMethod("render"); method.setAccessible(true); method.invoke(activity); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    private void snapshot(String name) {
        runOnMainSync(()->{
            View root=activity.getWindow().getDecorView();
            int width=root.getWidth(),height=root.getHeight();
            if(width==0||height==0) throw new AssertionError("Window has no layout");
            Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            root.draw(new Canvas(bitmap));
            try(FileOutputStream file=new FileOutputStream(new File(context.getExternalFilesDir(null),name))) {
                bitmap.compress(Bitmap.CompressFormat.PNG,100,file);
            } catch(IOException e) { throw new RuntimeException(e); }
            bitmap.recycle();
        });
    }
    private void checkNoClipping(View v) {
        if(v instanceof TextView) {
            TextView text=(TextView)v;
            if(text.getLayout()!=null) {
                for(int i=0;i<text.getLayout().getLineCount();i++) if(text.getLayout().getEllipsisCount(i)>0) throw new AssertionError("Truncated label: "+text.getText());
                if(text.getHeight()>0&&text.getLayout().getHeight()>text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom()+2) throw new AssertionError("Vertically clipped text: "+text.getText());
            }
        }
        if(v instanceof ViewGroup) { ViewGroup g=(ViewGroup)v; for(int i=0;i<g.getChildCount();i++) checkNoClipping(g.getChildAt(i)); }
    }
    private void inspect(View v) {
        if(v.getVisibility()!=View.VISIBLE) return;
        if(v.isAccessibilityHeading()) headings++;
        if(v instanceof Button) {
            Button b=(Button)v;
            if(b.getContentDescription()==null||b.getContentDescription().length()==0) throw new AssertionError("Unlabelled button");
            if(b.getMinHeight()<48*activity.getResources().getDisplayMetrics().density) throw new AssertionError("Small touch target");
            buttons++;
        }
        if(v instanceof ViewGroup) { ViewGroup g=(ViewGroup)v; for(int i=0;i<g.getChildCount();i++) inspect(g.getChildAt(i)); }
    }
}
