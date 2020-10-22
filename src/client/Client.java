package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.DisconnectedException;
import server.protocol.Command;

import java.io.*;
import java.net.*;

public class Client {
    public final static ObjectMapper mapper = new ObjectMapper();
    private final static int CONNECTION_TIMEOUT = 5000;

    private Socket socket;
    private final int port;
    private final String host;
    private PrintWriter outputStream;
    private BufferedReader inputStream;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;

        this.run();
    }


    private void run() {
        try {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(CONNECTION_TIMEOUT);

            this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outputStream = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

        } catch (UnknownHostException | ConnectException e) {
            System.out.println("Unknown host.");
        } catch (Exception e) {
            System.out.printf("Lost Connection to %s:%s%n", host, port);
            e.printStackTrace();

            this.cleanup();
        }
    }

    // test to see if connection to server is still ok by trying to
    // read the ACK byte the server returns to the client
    // set a timeout for the blocking read in case the server has died
    // or the connection has otherwise been broken. If no result is read
    // within the specified timeout the read method throws SocketTimeoutException
    // which will be passed up the call-stack to the nearest handler (catch block)
    // in the runClient method. If -1 is read, a common.DisconnectedException is thrown.
    private void testServerConnection() throws IOException, DisconnectedException {
        int old_timeout = socket.getSoTimeout();
        socket.setSoTimeout(CONNECTION_TIMEOUT);

        // this will throw an SocketTimeoutException if the connection to the server has been reset or times out
        int res = this.inputStream.read();

        if (res == -1) {
            throw new DisconnectedException(String.format("lost connection to %s:%s%n", host, port));
        }
        socket.setSoTimeout(old_timeout);
    }


    public JsonNode sendCommand(Command command, String... args) {
        JsonNode response = mapper.createObjectNode();

        this.outputStream.println(String.format("%s %s", command, String.join(" ", args)));
        this.outputStream.flush();


        try {
            var content = this.inputStream.readLine();

            response = mapper.readTree(content);

        } catch (SocketException e) {
            // When the stream throws a SocketException, this means that server
            // unexpectedly severed our connection. This could mean that the server
            // died or close the connection. In this case, we'll report this to the
            // user and invoke a clean-up operation.
            System.out.printf("Couldn't send '%s' command. Lost Connection to %s:%s%n", command, host, port);
            this.cleanup();

            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }


    private void cleanup() {
        try {
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}

