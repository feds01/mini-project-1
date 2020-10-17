

import common.DisconnectedException;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Client {

    private Socket socket;
    private final String host;
    private final int port;

    private BufferedReader br;
    private PrintWriter pw;

    private InputStream test_is;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;

        this.run();
    }


    private void run() {
        try {
            this.socket = new Socket(host, port);

            System.out.println("Client connected to " + host + " on port " + port + ".");
            System.out.println("To exit enter a single line containing: " + Configuration.exitString);

            br = new BufferedReader(new InputStreamReader(System.in));
            pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            test_is = socket.getInputStream();
            printUserInputToSocket(); // this runs until something goes wrong

        } catch (Exception e) {
            System.out.println("Oops on connection to " + host + " on port " + port + ". ");
            e.printStackTrace();

            cleanup(); // execute cleanup method to close connections cleanly
        }
    }

    // test to see if connection to server is still ok by trying to
    // read the ACK byte the server returns to the client
    // set a timeout for the blocking read in case the server has died
    // or the connection has otherwise been broken. If no result is read
    // within the specified timeout the read method throws SocketTimeoutException
    // which will be passed up the call-stack to the nearest handler (catch block)
    // in the runClient method. If -1 is read, a DisconnectedException is thrown.
    private void testServerConnection() throws IOException, DisconnectedException, SocketTimeoutException {
        int old_timeout = socket.getSoTimeout();
        socket.setSoTimeout(500);
        int res = test_is.read(); // this will throw an SocketTimeoutException if the connection to the server has been reset or times out
        if (res == -1) {
            throw new DisconnectedException("... connection has been lost ...");
        }
        socket.setSoTimeout(old_timeout);
    }

    private void printUserInputToSocket() throws IOException, SocketTimeoutException, DisconnectedException {
        while (true) {
            String line = br.readLine(); // get user input
            pw.println(line);            // print line out on the socket's output stream
            pw.flush();                  // flush the output stream so that the server gets message immediately
            if (line.equals(Configuration.exitString)) { // user has entered exit command
                throw new DisconnectedException(" ... user has entered exit command ... ");
            }
            testServerConnection(); // test to see if connection to server is still ok by trying to read the ACK byte the server returns to the client
        }
    }


    private void cleanup() {
        System.out.println("Client: ... cleaning up and exiting ... ");
        try {
            if (pw != null) pw.close();
            if (br != null) br.close();
            if (socket != null) socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
