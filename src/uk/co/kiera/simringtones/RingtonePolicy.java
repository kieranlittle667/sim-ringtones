package uk.co.kiera.simringtones;

import java.net.URI;
import java.net.URISyntaxException;

/** Limit selections to individual audio items in Android's local MediaStore. */
public final class RingtonePolicy {
    private RingtonePolicy() { }

    public static boolean isAllowedUri(String value) {
        if(value==null) return false;
        try {
            URI uri=new URI(value);
            String authority=uri.getRawAuthority();
            String path=uri.getRawPath();
            return "content".equals(uri.getScheme())
                    &&authority!=null&&authority.matches("(?:[0-9]+@)?media")
                    &&path!=null&&path.matches("/[A-Za-z0-9_-]+/audio/media/[0-9]+")
                    &&uri.getRawQuery()==null&&uri.getRawFragment()==null;
        } catch(URISyntaxException e) { return false; }
    }
}
