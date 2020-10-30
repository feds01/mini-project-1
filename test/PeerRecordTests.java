import server.PeerRecord;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PeerRecordTests {
    public static String VALID_ADDRESS = "127.0.0.1:12345";
    public static String VALID_HOST = "host";

    @Test
    public void testCreatingPeerRecord() {
        assertDoesNotThrow(() -> {
            new PeerRecord(VALID_ADDRESS, VALID_HOST, true);
        });
    }

    @Test
    public void testCreatingPeerRecordWithEmptyAddress() {
        assertThrows(IllegalArgumentException.class, () -> new PeerRecord("", VALID_HOST, true));
    }

    @Test
    public void testCreatingPeerRecordWithInvalidAddress() {
        assertThrows(IllegalArgumentException.class, () -> new PeerRecord("7127.1231.12.1", VALID_HOST, true));
    }

    @Test
    public void testToStringMethod() {
        var a = new PeerRecord(VALID_ADDRESS, VALID_HOST, true);
        var peerString = String.format("(RUNNING) Peer at %s on %s", VALID_ADDRESS, VALID_HOST);

        assertEquals(peerString, a.toString());
    }

    @Test
    public void testToStringMethodWithSelf() {
        var a = new PeerRecord(VALID_ADDRESS, VALID_HOST, true);

        a.setSelf(true);

        var peerString = String.format("(RUNNING) Peer at %s on %s (self)", VALID_ADDRESS, VALID_HOST);

        assertEquals(peerString, a.toString());
    }

    @Test
    public void testEqualsMethod() {
        var a = new PeerRecord(VALID_ADDRESS, VALID_HOST, true);
        var b = new PeerRecord(VALID_ADDRESS, VALID_HOST, true);

        assertEquals(b, a);
    }

}
