package uk.co.kiera.simringtones;

public final class RingtonePolicyTest {
    public static void main(String[] args) {
        String[] allowed={"content://media/internal/audio/media/12", "content://media/external/audio/media/42", "content://0@media/external_primary/audio/media/99"};
        String[] rejected={null,"", "file:///sdcard/tone.mp3", "https://example.com/tone.mp3", "content://evil/audio/media/1", "content://media.evil/internal/audio/media/1", "content://media/internal/audio/media/1?redirect=evil", "content://media/internal/audio/media/1#x", "content://media/internal/audio/media/../1", "content://media/internal/audio/media/%31", "content://media:80/internal/audio/media/1", "content://user@media/internal/audio/media/1", "content://media/internal/audio/media/-1", "content://media/internal/audio/media", "content://media/internal/images/media/1", "content://media/internal/audio/media/1/extra", "content://media/internal/audio/media/1\n"};
        for(String value:allowed) if(!RingtonePolicy.isAllowedUri(value)) throw new AssertionError("Rejected expected local audio: "+value);
        for(String value:rejected) if(RingtonePolicy.isAllowedUri(value)) throw new AssertionError("Accepted unsafe selection: "+value);
        System.out.println("PASS: "+(allowed.length+rejected.length)+" ringtone URI boundary cases");
    }
}
