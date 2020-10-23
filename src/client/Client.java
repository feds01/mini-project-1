package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.DisconnectedException;
import common.protocol.Command;

import java.io.*;
import java.net.*;

public class Client extends BaseConnection {
    public final static ObjectMapper mapper = new ObjectMapper();
    public final static int CONNECTION_TIMEOUT = 5000;

    public Client(String host, int port) {
        super(host, port);

        super.run();
    }

    // test to see if connection to server is still ok by trying to
    // read the ACK byte the server returns to the client
    // set a timeout for the blocking read in case the server has died
    // or the connection has otherwise been broken. If no result is read
    // within the specified timeout the read method throws SocketTimeoutException
    // which will be passed up the call-stack to the nearest handler (catch block)
    // in the runClient method. If -1 is read, a common.DisconnectedException is thrown.
    private void testServerConnection() throws IOException, DisconnectedException {
        int old_timeout = super.socket.getSoTimeout();
        super.socket.setSoTimeout(CONNECTION_TIMEOUT);

        // this will throw an SocketTimeoutException if the connection to the server has been reset or times out
        int res = super.inputStream.read();

        if (res == -1) {
            throw new DisconnectedException(String.format("lost connection to %s:%s%n", super.host, super.port));
        }
        super.socket.setSoTimeout(old_timeout);
    }


    public JsonNode sendCommand(Command command, String... args) {
        JsonNode response = mapper.createObjectNode();

        // Send over command with any additional arguments that are all separated by whitespaces.
        super.outputStream.println(String.format("%s %s", command, String.join(" ", args)));

        try {
            var content = super.inputStream.readLine();

            response = mapper.readTree(content);

        } catch (SocketException e) {
            // When the stream throws a SocketException, this means that server
            // unexpectedly severed our connection. This could mean that the server
            // died or close the connection. In this case, we'll report this to the
            // user and invoke a clean-up operation.
            System.out.printf("Couldn't send '%s' command. Lost Connection to %s:%s%n", command, super.host, super.port);
            super.cleanup();

            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
}

