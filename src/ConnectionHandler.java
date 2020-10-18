import common.DisconnectedException;

import java.io.*;
import java.net.Socket;

public class ConnectionHandler extends Thread {
    private final Socket connection;
    private InputStream in;
    private OutputStream out;
    private BufferedReader reader;
    private boolean running;

    public ConnectionHandler(Socket connection) {
        this.connection = connection;

        try {
            this.in = connection.getInputStream();
            this.out = connection.getOutputStream();
            this.reader = new BufferedReader(new InputStreamReader(this.in));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void run() {
        this.running = true;
        System.out.println("new ConnectionHandler thread started .... ");

        try {
            printClientData();
        } catch (Exception e) { // exit cleanly for any Exception (including IOException, ClientDisconnectedException)
            System.out.println("ConnectionHandler:run " + e.getMessage());
        } finally {
            this.cleanup();
        }
    }

    public void shutdown() {
        this.running = false;
    }

    private void printClientData() throws DisconnectedException, IOException {
        while (this.running) {
            var line = reader.readLine();


            // if readLine fails we can deduce here that the connection to the client is broken
            // and shut down the connection on this side cleanly by throwing a DisconnectedException
            // which will be passed up the call stack to the nearest handler (catch block)
            // in the run method
            if (line == null || line.equals("null") || line.equals(Configuration.exitString)) {
                throw new DisconnectedException(" ... client has closed the connection ... ");
            }
            // in this simple setup all the server does in response to messages from the client is to send
            // a single ACK byte back to client - the client uses this ACK byte to test whether the
            // connection to this server is still live, if not the client shuts down cleanly
            out.write(Configuration.ackByte);
            System.out.println("ConnectionHandler: " + line); // assuming no exception, print out line received from client
        }
    }


    private void cleanup() {

        System.out.println("ConnectionHandler: cleanup");
        try {
            reader.close();
            in.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ConnectionHandler: cleanup failed - " + e.getMessage());
        }
    }
}
