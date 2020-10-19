package server;

import common.Configuration;
import common.DisconnectedException;
import server.resources.DirectoryEntry;
import server.resources.FileEntry;
import server.resources.IEntry;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
        System.out.println("new server.ConnectionHandler thread started .... ");

        try {
            printClientData();
        } catch (Exception e) { // exit cleanly for any Exception (including IOException, ClientDisconnectedException)
            System.out.println("server.server.ConnectionHandler:run " + e.getMessage());
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
            // and shut down the connection on this side cleanly by throwing a common.DisconnectedException
            // which will be passed up the call stack to the nearest handler (catch block)
            // in the run method
            if (line == null || line.equals("null") || line.equals(Configuration.exitString)) {
                throw new DisconnectedException(" ... client has closed the connection ... ");
            }
            // in this simple setup all the server does in response to messages from the client is to send
            // a single ACK byte back to client - the client uses this ACK byte to test whether the
            // connection to this server is still live, if not the client shuts down cleanly
            out.write(Configuration.ackByte);
            System.out.println("server.ConnectionHandler: " + line); // assuming no exception, print out line received from client
        }
    }

    /**
     * Method to list the contents of the upload folder. This method will cycle through
     * all of the upload folder contents and convert each entry into either a DirectoryEntry
     * or FileEntry. FileEntry object has several methods which allows the caller to invoke
     * methods that can retrieve metadata from the file like the size or compute the md5 hash.
     * */
    private List<IEntry> listUploadFolderContents() {
        File uploadFolder = new File(Configuration.getInstance().get("upload"));

        List<IEntry> files = new ArrayList<>();

        // Loop through each entry in the upload folder and convert them into FileEntry objects
        for (File file : uploadFolder.listFiles()) {
            if (file.isFile()) {
                files.add( new FileEntry(file.toPath()));
            } else {
                files.add( new DirectoryEntry(file.toPath()));
            }
        }

        return files;
    }


    private void cleanup() {
        System.out.println("server.server.ConnectionHandler: cleanup");
        try {
            reader.close();
            in.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("server.server.ConnectionHandler: cleanup failed - " + e.getMessage());
        }
    }
}
