package uk.co.kiera.simringtones;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class UpdatePolicyTest {
    private static int checks;
    private static void check(boolean condition) { checks++; if(!condition) throw new AssertionError("Updater case "+checks); }
    interface Checked { void run() throws Exception; }
    private static void rejects(Checked operation) throws Exception {
        checks++;
        try { operation.run(); } catch(IOException e) { return; }
        throw new AssertionError("Expected refusal at updater case "+checks);
    }
    public static void main(String[] args) throws Exception {
        check(UpdatePolicy.version("v0.3.0")==UpdatePolicy.version("0.3.0"));
        check(UpdatePolicy.version("0.3.0")>UpdatePolicy.version("0.2.99"));
        check(UpdatePolicy.version("1.10.0")>UpdatePolicy.version("1.9.0"));
        for(String invalid:new String[]{null,"","v1.2","1.2.3-beta","1.2.3+build","01.2.3","1.1000.0","1.2.-1","9999999.0.0","1.2.3\n"}) check(UpdatePolicy.version(invalid)==-1);
        String url="https://github.com/kieranlittle667/sim-ringtones/releases/download/v0.3.0/sim-ringtones-0.3.0.apk";
        check(UpdatePolicy.downloadUrl(url,"v0.3.0","sim-ringtones-0.3.0.apk"));
        for(String invalid:new String[]{url.replace("https:","http:"),url.replace("github.com","github.com.evil"),url.replace("github.com","evil@github.com"),url.replace("kieranlittle667","other-owner"),url+"?redirect=evil",url+"#fragment",url.replace("0.3.0.apk","0.4.0.apk"),url.replace("github.com","github.com:444"),url.replace("sim-ringtones/releases","sim-ringtones/../other/releases")}) check(!UpdatePolicy.downloadUrl(invalid,"v0.3.0","sim-ringtones-0.3.0.apk"));
        for(String host:new String[]{"api.github.com","github.com","release-assets.githubusercontent.com","objects.githubusercontent.com"}) check(UpdatePolicy.transportUrl("https://"+host+"/path?signature=token"));
        for(String invalid:new String[]{"http://github.com/a","https://github.com.evil/a","https://evil.example/a","https://evil@github.com/a","https://github.com:444/a","file:///tmp/app.apk","https://github.com/a#fragment",null}) check(!UpdatePolicy.transportUrl(invalid));
        byte[] bytes="abc".getBytes(StandardCharsets.UTF_8);
        String digest="sha256:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),output,3,digest); check(output.toString(StandardCharsets.UTF_8).equals("abc"));
        rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),2,digest));
        rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),4,digest));
        rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),3,"sha256:"+"0".repeat(64)));
        rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),0,digest));
        rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),UpdatePolicy.MAX_APK_BYTES+1,digest));
        rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),3,"md5:abc"));
        check(UpdatePolicy.readLimited(new ByteArrayInputStream(bytes),3).length==3);
        rejects(()->UpdatePolicy.readLimited(new ByteArrayInputStream(bytes),2));
        Thread.currentThread().interrupt();
        try { rejects(()->UpdatePolicy.copyVerified(new ByteArrayInputStream(bytes),OutputStream.nullOutputStream(),3,digest)); }
        finally { Thread.interrupted(); }
        System.out.println("PASS: "+checks+" updater version, URL, size, checksum and cancellation cases");
    }
}
