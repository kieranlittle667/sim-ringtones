package uk.co.kiera.simringtones;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.telecom.*;
import android.telephony.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG=0xff101817, CARD=0xff1c2926, TEXT=0xfff1f7f3,
            MUTED=0xffbccdc5, ACCENT=0xff91e5d4;
    private LinearLayout page;
    private TextView status;
    private final Map<PhoneAccountHandle,TextView> currentLabels=new HashMap<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Ringtone playing;
    private PhoneAccountHandle pending;
    private String pendingLabel;
    private String statusText="Choose a ringtone, then call each SIM to test it.";
    private int sims;
    private UpdateController updates;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null) {
            pending=state.getParcelable("pending",PhoneAccountHandle.class);
            pendingLabel=state.getString("pendingLabel");
            statusText=state.getString("status",statusText);
        }
        updates=new UpdateController(this,state);
        render();
    }
    @Override public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("pending",pending);
        out.putString("pendingLabel",pendingLabel);
        out.putString("status",statusText);
        updates.saveState(out);
    }
    @Override protected void onResume() {
        super.onResume();
        // Refresh after returning from permissions or the system picker.
        render();
        updates.onResume();
    }
    @Override protected void onDestroy() { updates.close(); super.onDestroy(); }
    @Override protected void onPause() { stopPreview(); super.onPause(); }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private GradientDrawable surface(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); t.setLineSpacing(dp(3),1);
        if(bold) t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        return t;
    }
    private void add(LinearLayout box,View view,int gap) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(gap); box.addView(view,p);
    }
    private Button button(String value,String description,boolean primary,Runnable action) {
        Button b=new Button(this); b.setText(value); b.setAllCaps(false); b.setTextSize(16);
        b.setMinHeight(dp(52)); b.setMinimumHeight(dp(52)); b.setPadding(dp(16),dp(12),dp(16),dp(12));
        b.setTextColor(primary?BG:ACCENT); b.setBackgroundTintList(ColorStateList.valueOf(primary?ACCENT:0xff2b4039));
        b.setContentDescription(description); b.setOnClickListener(v->action.run()); return b;
    }
    private void render() {
        currentLabels.clear(); sims=0;
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22),dp(12),dp(22),dp(24));
        scroll.addView(page,new ScrollView.LayoutParams(-1,-2)); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,i)-> {
            android.graphics.Insets inset=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            v.setPadding(inset.left,inset.top,inset.right,inset.bottom); return i;
        }); scroll.requestApplyInsets();
        TextView eyebrow=text("YOUR LINES, YOUR SOUNDS",12,ACCENT,true); add(page,eyebrow,8);
        TextView title=text("SIM Ringtones",32,TEXT,true); title.setAccessibilityHeading(true); add(page,title,12);
        add(page,text("Hear which line is calling. Choose a different sound for each SIM.",17,MUTED,false),6);
        boolean phone=checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED
                &&checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS)==PackageManager.PERMISSION_GRANTED;
        boolean write=Settings.System.canWrite(this);
        LinearLayout ready=new LinearLayout(this); ready.setOrientation(LinearLayout.VERTICAL);
        ready.setPadding(dp(16),dp(12),dp(16),dp(12)); ready.setBackground(surface(0xff203a31,16));
        add(ready,text(phone&&write?"Ready to personalise":"Finish setup",16,ACCENT,true),0);
        add(ready,text(phone&&write?"Changes saved directly to your phone":"Allow access to SIM names and ringtone settings.",14,TEXT,false),2);
        if(!phone) add(ready,button("Allow SIM access","Allow access to SIM names",true,()->requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_PHONE_NUMBERS},10)),8);
        if(!write) add(ready,button("Allow ringtone changes","Allow SIM Ringtones to change system settings",true,()->startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())))),8);
        add(page,ready,20);
        if(phone) {
            try {
                TelecomManager telecom=getSystemService(TelecomManager.class);
                List<PhoneAccountHandle> handles=new ArrayList<>(telecom.getCallCapablePhoneAccounts());
                handles.removeIf(h->{ PhoneAccount a=telecom.getPhoneAccount(h); return a==null||!a.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION); });
                handles.sort(Comparator.comparingInt(this::slot));
                for(PhoneAccountHandle h:handles) { addSim(h,telecom.getPhoneAccount(h),write); sims++; }
                if(sims==0) add(page,text("No active voice SIMs found. Turn on a SIM in Android settings, then refresh.",17,TEXT,false),20);
            } catch(RuntimeException | LinkageError e) { statusText="Could not load SIMs. Check SIM permissions and refresh."; }
        }
        status=text(statusText,15,MUTED,false); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); add(page,status,20);
        add(page,button("Refresh SIMs","Refresh SIMs and current ringtones",false,()->{render();announce("SIM list refreshed");}),12);
        add(page,text("Ringtones are saved on this device. No account or background service.",13,MUTED,false),16);
        addUpdates();
    }
    private void addUpdates() {
        TextView heading=text("Updates",21,TEXT,true); heading.setAccessibilityHeading(true); add(page,heading,24);
        add(page,text("Checks GitHub for new releases when you open the app, at most once a day. Downloads and installation require your confirmation.",14,MUTED,false),6);
        CheckBox automatic=new CheckBox(this); automatic.setText("Check for updates automatically"); automatic.setContentDescription("Check for updates automatically"); automatic.setTextSize(16); automatic.setTextColor(TEXT);
        automatic.setButtonTintList(ColorStateList.valueOf(ACCENT)); automatic.setMinHeight(dp(52)); automatic.setPadding(dp(4),dp(8),dp(4),dp(8)); add(page,automatic,8);
        TextView updateStatus=text("",15,MUTED,false); updateStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); add(page,updateStatus,8);
        Button check=button("Check for updates","Check for SIM Ringtones updates",false,()->{}); add(page,check,8);
        TextView notes=text("",14,MUTED,false); add(page,notes,8);
        Button action=button("Download update","Download SIM Ringtones update",true,()->{}); add(page,action,8);
        updates.bind(updateStatus,notes,check,action,automatic);
    }
    private int slot(PhoneAccountHandle h) {
        try {
            int id=getSystemService(TelephonyManager.class).getSubscriptionId(h);
            SubscriptionInfo info=getSystemService(SubscriptionManager.class).getActiveSubscriptionInfo(id);
            return info!=null?info.getSimSlotIndex():99;
        } catch(RuntimeException e) { return 99; }
    }
    private String label(PhoneAccountHandle h,PhoneAccount account) {
        int slot=slot(h);
        String name=account.getLabel()==null?"Mobile line":account.getLabel().toString();
        return (slot==99?"SIM":"SIM "+(slot+1))+" · "+name;
    }
    private String name(Uri uri) {
        if(uri==null) return "Silent";
        try { Ringtone r=RingtoneManager.getRingtone(this,uri); return r==null?"Selected ringtone":r.getTitle(this); }
        catch(RuntimeException e) { return "Selected ringtone"; }
    }
    private Uri current(PhoneAccountHandle h) { return RingtoneManager.getRingtoneUriForPhoneAccountHandle(this,h); }
    // Android 17's per-account key, used only to distinguish inherited vs explicit
    // values for the status label and exact rollback. All user changes use the API.
    private String key(PhoneAccountHandle h) { return "ringtone_id_"+h.getId(); }
    private String summary(PhoneAccountHandle h) {
        String raw=Settings.System.getString(getContentResolver(),key(h));
        return (raw==null?"Phone default: ":"Ringtone: ")+name(current(h));
    }
    private void addSim(PhoneAccountHandle h,PhoneAccount account,boolean write) {
        String line=label(h,account);
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18),dp(16),dp(18),dp(16)); card.setBackground(surface(CARD,22));
        TextView heading=text(line,21,TEXT,true); heading.setAccessibilityHeading(true); add(card,heading,0);
        TextView choice=text(summary(h),16,MUTED,false); choice.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        currentLabels.put(h,choice); add(card,choice,5);
        Button choose=button("Choose ringtone","Choose ringtone for "+line,true,()->choose(h,line));
        choose.setEnabled(write); add(card,choose,12);
        Button preview=button("Preview ringtone","Preview ringtone for "+line,false,()->preview(h,line));
        add(card,preview,3);
        Button reset=button("Use phone default","Use phone default ringtone for "+line,false,()->new AlertDialog.Builder(this)
                .setTitle("Use phone default?").setMessage(line+" will follow your phone's main ringtone setting.")
                .setNegativeButton("Cancel",null).setPositiveButton("Use phone default",(d,w)->save(h,line,null)).show());
        reset.setEnabled(write); add(card,reset,3);
        add(page,card,16);
    }
    private void choose(PhoneAccountHandle h,String label) {
        stopPreview(); pending=null; pendingLabel=null;
        if(!isActiveSim(h)) { announce("This SIM is no longer available. Refresh SIMs first."); return; }
        try {
        Intent intent=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Ringtone for "+label);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,current(h));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,false);
        // Android falls back to the phone default for a null per-account value.
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);
            ResolveInfo picker=getPackageManager().resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);
            if(picker==null||picker.activityInfo==null
                    ||(picker.activityInfo.applicationInfo.flags
                    &(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))==0) {
                announce("No system ringtone picker is available."); return;
            }
            intent.setComponent(new ComponentName(picker.activityInfo.packageName,picker.activityInfo.name));
            pending=h; pendingLabel=label;
            startActivityForResult(intent,20);
        } catch(RuntimeException e) {
            pending=null; pendingLabel=null;
            announce("Could not open the system ringtone picker.");
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==20) {
            try {
                if(result==RESULT_OK&&data!=null&&pending!=null) {
                    Uri uri=data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI,Uri.class);
                    if(uri!=null) save(pending,pendingLabel,uri);
                    else announce("No ringtone selected. Your ringtone has not changed.");
                }
            } catch(RuntimeException e) {
                announce("The picker returned an invalid selection. Your ringtone has not changed.");
            } finally {
                pending=null; pendingLabel=null;
            }
        }
    }
    private boolean isActiveSim(PhoneAccountHandle h) {
        if(h==null||h.getId()==null||h.getId().isEmpty()) return false;
        try {
            TelecomManager telecom=getSystemService(TelecomManager.class);
            if(telecom==null||!telecom.getCallCapablePhoneAccounts().contains(h)) return false;
            PhoneAccount account=telecom.getPhoneAccount(h);
            return account!=null&&account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);
        } catch(RuntimeException e) { return false; }
    }
    private static final class SaveFailure extends RuntimeException {
        SaveFailure(String message) { super(message); }
    }
    private boolean restoreSetting(String setting,String original) {
        try {
            if(!Objects.equals(Settings.System.getString(getContentResolver(),setting),original)) {
                if(!Settings.System.putString(getContentResolver(),setting,original)) return false;
            }
            return Objects.equals(Settings.System.getString(getContentResolver(),setting),original);
        } catch(RuntimeException e) { return false; }
    }
    private void save(PhoneAccountHandle h,String line,Uri uri) {
        if(!Settings.System.canWrite(this)) { announce("Allow ringtone changes first."); return; }
        // A null account tells Android to change the global ringtone instead.
        if(!isActiveSim(h)) { announce("This SIM is no longer available. Refresh SIMs first."); return; }
        if(uri!=null&&!RingtonePolicy.isAllowedUri(uri.toString())) {
            announce("Choose a built-in ringtone or a local sound added to Android's ringtone library."); return;
        }
        String global,before;
        try {
            global=Settings.System.getString(getContentResolver(),Settings.System.RINGTONE);
            before=Settings.System.getString(getContentResolver(),key(h));
        } catch(RuntimeException e) { announce("Could not read ringtone settings. Check permissions and try again."); return; }
        try {
            RingtoneManager.setRingtoneUri(this,uri,h);
            String after=Settings.System.getString(getContentResolver(),key(h));
            String globalAfter=Settings.System.getString(getContentResolver(),Settings.System.RINGTONE);
            if(!Objects.equals(global,globalAfter)) {
                throw new SaveFailure("This Android build changed the main ringtone instead of the SIM ringtone.");
            }
            if(uri!=null&&(after==null||!sameAudio(Uri.parse(after),uri))) {
                throw new SaveFailure("Android did not accept the selected sound. Try a built-in ringtone.");
            }
            if(uri==null&&after!=null) throw new SaveFailure("Android did not clear the SIM ringtone.");
            TextView choice=currentLabels.get(h); if(choice!=null) choice.setText(summary(h));
            announce(line+" saved. "+summary(h)+". Call this SIM to test.");
        } catch(RuntimeException | LinkageError e) {
            boolean globalRestored=restoreSetting(Settings.System.RINGTONE,global);
            boolean simRestored=restoreSetting(key(h),before);
            if(!globalRestored||!simRestored) {
                announce("Could not restore ringtone settings. Check the phone default and each SIM's ringtone in Android settings.");
            } else {
                announce("Previous ringtone settings restored. "+(e instanceof SaveFailure?e.getMessage():"Check permissions and try a built-in ringtone."));
            }
        }
    }
    private boolean sameAudio(Uri a,Uri b) {
        // The framework may add the current Android user's prefix to external URIs.
        return a.toString().replace("content://0@","content://").equals(b.toString().replace("content://0@","content://"));
    }
    private void preview(PhoneAccountHandle h,String line) {
        if(playing!=null) { stopPreview(); announce("Preview stopped."); return; }
        stopPreview();
        try {
            Uri uri=current(h); if(uri==null) { announce(line+" is silent."); return; }
            playing=RingtoneManager.getRingtone(this,uri);
            if(playing==null) { announce("This sound cannot be previewed."); return; }
            playing.setLooping(false); playing.play();
            announce("Previewing "+line+". Tap Preview ringtone again to stop.");
            handler.postDelayed(this::stopPreview,8000);
        } catch(RuntimeException e) { stopPreview(); announce("Could not preview this ringtone."); }
    }
    private void stopPreview() {
        handler.removeCallbacksAndMessages(null);
        if(playing!=null) { try { playing.stop(); } catch(RuntimeException ignored) { } finally { playing=null; } }
    }
    private void announce(String value) { statusText=value; if(status!=null) status.setText(value); }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results); render();
    }
}
