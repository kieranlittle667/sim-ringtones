package uk.co.kiera.simringtones;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Trust boundaries shared by the updater and its host-side tests. */
public final class UpdatePolicy {
    public static final long MAX_APK_BYTES=50L*1024*1024;
    public static final int MAX_JSON_BYTES=1024*1024;
    private UpdatePolicy() { }

    public static long version(String value) {
        if(value==null||!value.matches("v?(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,2})\\.(0|[1-9][0-9]{0,2})")) return -1;
        String[] parts=value.replaceFirst("^v", "").split("\\.");
        return Long.parseLong(parts[0])*1000000+Long.parseLong(parts[1])*1000+Long.parseLong(parts[2]);
    }
    public static boolean digest(String value) {
        return value!=null&&value.matches("sha256:[a-fA-F0-9]{64}");
    }
    public static boolean downloadUrl(String value,String tag,String name) {
        if(version(tag)<0||name==null||!name.equals("sim-ringtones-"+tag.replaceFirst("^v", "")+".apk")) return false;
        try {
            URI uri=new URI(value);
            return cleanHttps(uri)&&"github.com".equals(uri.getHost())
                    &&uri.getRawPath().equals("/kieranlittle667/sim-ringtones/releases/download/"+tag+"/"+name)
                    &&uri.getRawQuery()==null;
        } catch(URISyntaxException|NullPointerException e) { return false; }
    }
    public static boolean transportUrl(String value) {
        try {
            URI uri=new URI(value);
            if(!cleanHttps(uri)) return false;
            String host=uri.getHost();
            return "api.github.com".equals(host)||"github.com".equals(host)
                    ||"release-assets.githubusercontent.com".equals(host)
                    ||"objects.githubusercontent.com".equals(host);
        } catch(URISyntaxException|NullPointerException e) { return false; }
    }
    private static boolean cleanHttps(URI uri) {
        return "https".equals(uri.getScheme())&&uri.getRawUserInfo()==null
                &&(uri.getPort()==-1||uri.getPort()==443)&&uri.getRawFragment()==null;
    }
    public static byte[] readLimited(InputStream input,int limit) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] buffer=new byte[8192]; int count;
        while((count=input.read(buffer))!=-1) {
            if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if(output.size()+(long)count>limit) throw new IOException("Response is too large");
            output.write(buffer,0,count);
        }
        return output.toByteArray();
    }
    public static void copyVerified(InputStream input,OutputStream output,long expectedSize,String expectedDigest) throws IOException {
        if(expectedSize<=0||expectedSize>MAX_APK_BYTES||!digest(expectedDigest)) throw new IOException("Invalid asset metadata");
        try {
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            byte[] buffer=new byte[32768]; long total=0; int count;
            while((count=input.read(buffer))!=-1) {
                if(Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                total+=count;
                if(total>expectedSize) throw new IOException("Download is too large");
                hash.update(buffer,0,count); output.write(buffer,0,count);
            }
            if(total!=expectedSize||!HexFormat.of().formatHex(hash.digest()).equalsIgnoreCase(expectedDigest.substring(7))) {
                throw new IOException("Download verification failed");
            }
        } catch(NoSuchAlgorithmException e) { throw new IOException("SHA-256 unavailable",e); }
    }
}
