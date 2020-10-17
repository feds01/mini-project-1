import common.DisconnectedException;

import java.io.*;
import java.net.Socket;

public class ConnectionHandler extends Thread {
    private final Socket connection;
    private InputStream in;
    private OutputStream out;
    private BufferedReader reader;

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
    public void run() { // run method is invoked when the Thread's start method (ch.start(); in Server class) is invoked
        System.out.println("new ConnectionHandler thread started .... ");
        try {
            printClientData();
        } catch (Exception e) { // exit cleanly for any Exception (including IOException, ClientDisconnectedException)
            System.out.println("ConnectionHandler:run " + e.getMessage());
            cleanup();     // cleanup and exit
        }
    }

    private void printClientData() throws DisconnectedException, IOException {
        while (true) {
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


    public void cleanup() {

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
