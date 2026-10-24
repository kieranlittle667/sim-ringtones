package uk.co.kiera.simringtones;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Network and archive verification. All methods run on the updater worker. */
final class UpdateClient {
    static final String LATEST="https://api.github.com/repos/kieranlittle667/sim-ringtones/releases/latest";
    static final class Release {
        final String tag,url,digest,notes;
        final long size;
        Release(String tag,String url,String digest,String notes,long size) {
            this.tag=tag; this.url=url; this.digest=digest; this.notes=notes; this.size=size;
        }
    }
    static Release parseRelease(JSONObject json,String currentVersion) throws JSONException,IOException {
        if(json.getBoolean("draft")||json.getBoolean("prerelease")) return null;
        String tag=json.getString("tag_name");
        long version=UpdatePolicy.version(tag),current=UpdatePolicy.version(currentVersion);
        if(version<0||current<0) throw new IOException("Unsupported release version");
        if(version<=current) return null;
        String name="sim-ringtones-"+tag.replaceFirst("^v", "")+".apk";
        JSONArray assets=json.getJSONArray("assets");
        Release found=null;
        for(int i=0;i<assets.length();i++) {
            JSONObject asset=assets.getJSONObject(i);
            if(!name.equals(asset.optString("name"))) continue;
            String url=asset.getString("browser_download_url"),digest=asset.getString("digest");
            long size=asset.getLong("size");
            if(found!=null||!"uploaded".equals(asset.getString("state"))||!UpdatePolicy.downloadUrl(url,tag,name)
                    ||!UpdatePolicy.digest(digest)||size<=0||size>UpdatePolicy.MAX_APK_BYTES) {
                throw new IOException("Unsafe release asset");
            }
            String notes=json.optString("body","");
            if(notes.length()>4000) notes=notes.substring(0,4000)+"…";
            found=new Release(tag,url,digest,notes,size);
        }
        if(found==null) throw new IOException("No compatible APK in this release");
        return found;
    }
    static Release check(String currentVersion) throws IOException,JSONException {
        HttpsURLConnection connection=open(LATEST);
        try(InputStream input=connection.getInputStream()) {
            String json=new String(UpdatePolicy.readLimited(input,UpdatePolicy.MAX_JSON_BYTES),StandardCharsets.UTF_8);
            return parseRelease(new JSONObject(json),currentVersion);
        } finally { connection.disconnect(); }
    }
    private static HttpsURLConnection open(String value) throws IOException {
        for(int redirects=0;redirects<=5;redirects++) {
            if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if(!UpdatePolicy.transportUrl(value)) throw new IOException("Untrusted update host");
            URL url=new URL(value);
            HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent","SIM-Ringtones-Updater");
            connection.setRequestProperty("Accept","application/vnd.github+json");
            connection.setRequestProperty("Accept-Encoding","identity");
            try {
                int status=connection.getResponseCode();
                if(status==200) return connection;
                if(status==301||status==302||status==303||status==307||status==308) {
                    String location=connection.getHeaderField("Location");
                    if(location==null) throw new IOException("Missing redirect");
                    value=new URL(url,location).toString();
                } else { throw new IOException("Update server unavailable"); }
            } catch(IOException|RuntimeException e) { connection.disconnect(); throw e; }
            connection.disconnect();
        }
        throw new IOException("Too many redirects");
    }
    static File download(Context context,Release release) throws IOException {
        File directory=new File(context.getCacheDir(),"updates");
        if(!directory.isDirectory()&&!directory.mkdirs()) throw new IOException("Cannot create update cache");
        File partial=File.createTempFile("download-",".apk",directory),apk=new File(directory,"update.apk");
        HttpsURLConnection connection=null;
        boolean complete=false;
        try {
            connection=open(release.url);
            long length=connection.getContentLengthLong();
            if(length!=-1&&length!=release.size) throw new IOException("Unexpected download size");
            try(InputStream input=connection.getInputStream();OutputStream output=new FileOutputStream(partial)) {
                UpdatePolicy.copyVerified(input,output,release.size,release.digest);
            }
            verifyArchive(context,partial,release);
            if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if(apk.exists()&&!apk.delete()) throw new IOException("Cannot replace cached update");
            if(!partial.renameTo(apk)) throw new IOException("Cannot save verified update");
            complete=true;
            return apk;
        } finally {
            if(connection!=null) connection.disconnect();
            if(!complete) partial.delete();
        }
    }
    static void verifyArchive(Context context,File apk,Release release) throws IOException {
        PackageManager pm=context.getPackageManager();
        PackageInfo incoming=pm.getPackageArchiveInfo(apk.getPath(),PackageManager.GET_SIGNING_CERTIFICATES);
        try {
            PackageInfo installed=pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
            if(incoming==null||!context.getPackageName().equals(incoming.packageName)
                    ||incoming.getLongVersionCode()<=installed.getLongVersionCode()
                    ||UpdatePolicy.version(incoming.versionName)!=UpdatePolicy.version(release.tag)
                    ||incoming.applicationInfo==null||incoming.applicationInfo.minSdkVersion>android.os.Build.VERSION.SDK_INT
                    ||incoming.signingInfo==null||installed.signingInfo==null) throw new IOException("Incompatible update");
            Signature[] candidate=incoming.signingInfo.getApkContentsSigners(),current=installed.signingInfo.getApkContentsSigners();
            // Reject unavailable certificates and key changes, even if Android could accept a lineage.
            if(candidate==null||current==null||candidate.length!=1||current.length!=1
                    ||!Arrays.equals(candidate[0].toByteArray(),current[0].toByteArray())) {
                throw new IOException("Update signing certificate does not match");
            }
        } catch(PackageManager.NameNotFoundException e) { throw new IOException("Installed app unavailable",e); }
    }
}
