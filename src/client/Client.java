package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.protocol.Command;

import java.io.*;
import java.net.*;

/**
 *
 * */
public class Client extends BaseConnection {
    /**
     *
     * */
    public final static ObjectMapper mapper = new ObjectMapper();

    /**
     *
     * */
    public Client(String host, int port) {
        super(host, port);

        super.run();
    }


    /**
     *
     * */
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

