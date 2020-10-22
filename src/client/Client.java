package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.DisconnectedException;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

public class Client {
    public final static ObjectMapper mapper = new ObjectMapper();
    private final static List<String> CLIENT_COMMANDS = List.of("get", "list", "peers");
    private final static int CONNECTION_TIMEOUT = 500;

    private Socket socket;
    private final int port;
    private final String host;
    private PrintWriter outputStream;
    private DataInputStream inputStream;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;

        this.run();
    }


    private void run() {
        try {
            this.socket = new Socket(host, port);

            this.inputStream = new DataInputStream(socket.getInputStream());
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


    public JsonNode sendCommand(String command) {
        JsonNode response = mapper.createObjectNode();

        // check that the command is a valid and defined command.
        if (!CLIENT_COMMANDS.contains(command)) {
            throw new IllegalArgumentException("Invalid client command.");
        }

        this.outputStream.println(command);
        this.outputStream.flush();


        try {
            ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int chunk;

            while ((chunk = this.inputStream.read(buffer, 0, 1024)) > 0) {

                // If the server sends a carriage return, that's the end of the response.
                // TODO: do this in a much cleaner way.
                if (buffer[chunk - 1] == '\r') {
                    break;
                }

                bufferStream.write(buffer, 0, chunk);
            }

            bufferStream.flush();
            response = mapper.readTree(bufferStream.toByteArray());

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

