package common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.regex.Pattern;


/**
 * Class that holds utility networking methods that the application uses.
 *
 * @author 200008575
 */
public class Networking {
    /**
     * Method used to acquire a free port on the system by starting a socket
     * server with port zero, which means that it is allocated a free port on
     * the system. After getting the port number from the ServerSocket, it will
     * immediately close it and return the port number. This method was used from
     * an online source.
     *
     * @return the number of the free port.
     * @see <a href="https://stackoverflow.com/a/2675416/9955666">https://stackoverflow.com/a/2675416/9955666</a>
     */
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

    /**
     * This method will use a regular expression to convert a String which
     * represents an IPv4 address. It will return a {@link InetSocketAddress}
     * that is formed from parsing the actual address and the port number from
     * the address.
     *
     * @param address The string representation of an IPv4 address.
     * @return A {@link InetSocketAddress} which is formed from the string address.
     * @throws IllegalArgumentException if the string is not recognised as an
     *                                  IPv4 address.
     */
    public static InetSocketAddress parseAddressFromString(String address) {
        var pattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})");
        var matcher = pattern.matcher(address);

        if (matcher.matches()) {
            int port = Integer.parseInt(matcher.group(2));

            return new InetSocketAddress(matcher.group(1), port);
        } else {
            throw new IllegalArgumentException("Invalid address.");
        }
    }
}
