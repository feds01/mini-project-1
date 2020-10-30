import common.Networking;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkingUtilityTests {
    @Test
    public void testGettingFreePort() {
        assertNotEquals(0, Networking.getFreePort());
    }

    @Test
    public void testParseIncorrectAddress() {
        String address = "Invalid address";

        assertThrows(IllegalArgumentException.class, () -> Networking.parseAddressFromString(address));
    }

    @Test
    public void testParseAddressWithoutPort() {
        String address = "127.0.0.1";

        assertThrows(IllegalArgumentException.class, () -> Networking.parseAddressFromString(address));
    }

    @Test
    public void testParseAddressWithTooLongPort() {
        String address = "127.0.0.1:2193312";

        assertThrows(IllegalArgumentException.class, () -> Networking.parseAddressFromString(address));
    }

    @Test
    public void testParseAddress() {
        String address = "127.0.0.1:12345";

        var parsedAddr = Networking.parseAddressFromString(address);

        assertEquals(parsedAddr.getAddress().getHostAddress(), "127.0.0.1");
        assertEquals(parsedAddr.getPort(), 12345);
    }
}
