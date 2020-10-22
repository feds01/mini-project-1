package common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.regex.Pattern;

public class Networking {

    // https://stackoverflow.com/a/2675416/9955666
    public static int getFreePort() {
        int port = 0;

        try {
            var socketServer = new ServerSocket(0);

            port = socketServer.getLocalPort();


            socketServer.close();

            return port;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return port;
    }

    public static InetSocketAddress parseAddressFromString(String address) {
        var pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)");
        var matcher = pattern.matcher(address);

        if (matcher.matches()) {
            if (matcher.group(1) != null && matcher.group(2) != null) {
                int port = Integer.parseInt(matcher.group(2));

                return new InetSocketAddress(matcher.group(1), port);
            }
        } else {
            throw new IllegalArgumentException("Invalid address.");
        }

        return null;
    }
}
