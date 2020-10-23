package client;

import common.Configuration;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

public class BaseConnection {
    public final static int CONNECTION_TIMEOUT = 5000;
    public final static Configuration config = Configuration.getInstance();

    protected Socket socket;
    protected final int port;
    protected final String host;
    protected PrintWriter outputStream;
    protected BufferedReader inputStream;

    public BaseConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        try {
            this.socket = new Socket(this.host, port);
            this.socket.setSoTimeout(CONNECTION_TIMEOUT);

            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.outputStream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        } catch (UnknownHostException | ConnectException e) {
            System.out.println("Unknown host.");
        } catch (Exception e) {
            System.out.printf("Lost Connection to %s:%s%n", host, port);
            e.printStackTrace();

            this.cleanup();
        }
    }

    public void cleanup() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
