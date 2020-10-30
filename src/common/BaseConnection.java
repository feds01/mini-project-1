package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Abstract base class for a generic connection that involves a socket connection.
 *
 * @author 200008575
 */
public abstract class BaseConnection {
    /**
     * Number that represents the timeout (in milliseconds) of the connection.
     */
    public final static int CONNECTION_TIMEOUT = 10000;

    /**
     * Instance of the configuration object which is used to get application settings.
     */
    public final static Configuration config = Configuration.getInstance();

    /**
     * The socket that the connection is interacting with.
     */
    protected Socket socket;

    /**
     * The hostname of the remote connection that will be made in the
     * socket.
     */
    protected final String host;

    /**
     * The port on the hostname of the remote connection that will be made in the
     * socket.
     */
    protected final int port;

    /**
     * The output stream of the connection that the ConnectionHandler is managing.
     */
    protected PrintWriter printWriter;

    /**
     * The reader of the input stream which is used to listen for commands from the
     * peer connection.
     */
    protected BufferedReader bufferedReader;

    /**
     * Constructor method for a BaseConnection. The method will initialise the socket
     * and open the socket I/O streams ready for classes that build on the abstract class
     * to use.
     */
    public BaseConnection(String host, int port) throws IOException {
        this.host = host;
        this.port = port;

        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(CONNECTION_TIMEOUT); // set the connection timeout to our defined time.

        // open the socket streams
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    /**
     * Initialise the BaseConnection with a pre-existing socket.
     */
    public BaseConnection(Socket socket) throws IOException {
        this.port = socket.getLocalPort();
        this.host = socket.getInetAddress().getHostName();

        this.socket = socket;

        // open the socket streams
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

    }

    /**
     * Method used to clean up any resources when closing the connection.
     */
    public void cleanup() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (printWriter != null) printWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to get the port number of the current connection.
     *
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    /**
     * Method to get the host of the current connection
     *
     * @return The host.
     */
    public String getHost() {
        return host;
    }


    /**
     * This method combines the host and the port of the current
     * connection into an IPv4 address format.
     *
     * @return A String representing the address based of the connection's
     * host and port
     */
    public String getAddress() {
        return this.host + ":" + this.port;
    }
}
