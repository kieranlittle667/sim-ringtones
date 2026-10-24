package uk.co.kiera.simringtones;

import org.json.*;
import java.io.IOException;

public final class ReleaseMetadataTest {
    private static int checks;
    private static JSONObject release() {
        JSONObject asset=new JSONObject().put("name","sim-ringtones-0.3.0.apk").put("state","uploaded").put("size",12345)
                .put("digest","sha256:"+"a".repeat(64))
                .put("browser_download_url","https://github.com/kieranlittle667/sim-ringtones/releases/download/v0.3.0/sim-ringtones-0.3.0.apk");
        return new JSONObject().put("draft",false).put("prerelease",false).put("tag_name","v0.3.0").put("body","A release")
                .put("assets",new JSONArray().put(asset));
    }
    private static void check(boolean valid) { checks++; if(!valid) throw new AssertionError("Release case "+checks); }
    private static void refused(JSONObject json) throws Exception {
        checks++;
        try { UpdateClient.parseRelease(json,"0.2.0"); } catch(IOException|JSONException e) { return; }
        throw new AssertionError("Unsafe metadata accepted at release case "+checks);
    }
    public static void main(String[] args) throws Exception {
        JSONObject json=release();
        check(UpdateClient.parseRelease(json,"0.2.0").tag.equals("v0.3.0"));
        check(UpdateClient.parseRelease(json,"0.3.0")==null);
        check(UpdateClient.parseRelease(json,"0.4.0")==null);
        check(UpdateClient.parseRelease(release().put("draft",true),"0.2.0")==null);
        check(UpdateClient.parseRelease(release().put("prerelease",true),"0.2.0")==null);
        refused(release().put("tag_name","v0.3.0-beta"));
        refused(release().put("assets",new JSONArray()));
        json=release(); json.getJSONArray("assets").put(json.getJSONArray("assets").getJSONObject(0)); refused(json);
        for(String field:new String[]{"digest","size","state","browser_download_url"}) { json=release(); json.getJSONArray("assets").getJSONObject(0).remove(field); refused(json); }
        for(long size:new long[]{0,-1,UpdatePolicy.MAX_APK_BYTES+1}) { json=release(); json.getJSONArray("assets").getJSONObject(0).put("size",size); refused(json); }
        json=release(); json.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://evil.example/app.apk"); refused(json);
        json=release(); json.getJSONArray("assets").getJSONObject(0).put("digest","sha256:missing"); refused(json);
        json=release(); json.getJSONArray("assets").getJSONObject(0).put("state","new"); refused(json);
        check(UpdateClient.parseRelease(release().put("body","x".repeat(10000)),"0.2.0").notes.length()==4001);
        refused(new JSONObject());
        System.out.println("PASS: "+checks+" release metadata, downgrade, missing asset and prerelease cases");
    }
}
