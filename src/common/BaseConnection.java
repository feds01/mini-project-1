package common;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Abstract base class for a generic connection that involves a socket connection.
 *
 * @author 200008575
 */
public abstract class BaseConnection {
    /**
     * Number that represents the timeout (in milliseconds) of the connection.
     */
    public final static int CONNECTION_TIMEOUT = 5000;

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
     *
     * Constructor method for a BaseConnection. The method will initialise the socket
     * and open the socket I/O streams ready for classes that build on the abstract class
     * to use.
     */
    public BaseConnection(String host, int port) {
        this.host = host;
        this.port = port;


        try {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(CONNECTION_TIMEOUT); // set the connection timeout to our defined time.

            // open the socket streams
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        } catch (UnknownHostException | ConnectException e) {
            System.out.println("Unknown host.");
        } catch (Exception e) {
            System.out.printf("Lost Connection to %s:%s%n", host, port);
            e.printStackTrace();

            this.cleanup();
        }
    }

    /**
     * Initialise the BaseConnection with a pre-existing socket.
     * */
    public BaseConnection(Socket socket) {
        this.port = socket.getLocalPort();
        this.host = socket.getInetAddress().getHostName();

        try {
            this.socket = socket;

            // open the socket streams
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        } catch (UnknownHostException | ConnectException e) {
            System.out.println("Unknown host.");
        } catch (Exception e) {
            System.out.printf("Lost Connection to %s:%s%n", host, port);
            e.printStackTrace();

            this.cleanup();
        }
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
}
