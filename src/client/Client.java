package client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.BaseConnection;
import common.protocol.Command;

import java.io.*;
import java.net.*;

/**
 * Client class that is used to relay requests to the connected
 * peer and returns responses back to the commander.
 *
 * @author 200008575
 * */
public class Client extends BaseConnection {
    /**
     * An instance of a Jackson ObjectMapper, used to deserialize responses that
     * are sent by the peer
     */
    public final static ObjectMapper mapper = new ObjectMapper();

    /**
     * Client class constructor
     *
     * @param host - The hostname of the peer
     * @param port - The port on the hostname of the peer
     * */
    public Client(String host, int port) {
        super(host, port);
    }

    /**
     * This method is used to send commands that are defined in the protocol with
     * additional arguments to the connected peer. The method will also wait for
     * a response from the peer that will be deserialized via the Jackson library
     * into a JsonNode. If the peer drops the connection during a transmission, the
     * super class cleanup function will cleanup the open resources.
     *
     * @param command - The base command that is used for the request
     * @param args - An array of any size that represent arguments that will
     *             be sent with the base command to give context to the request.
     *
     * @return A {@link JsonNode} that represent the deserialized response from the
     *         peer.
     * */
    public JsonNode sendCommand(Command command, String... args) {
        JsonNode response = mapper.createObjectNode();

        // Send over command with any additional arguments that are all separated by whitespaces.
        this.printWriter.println(String.format("%s %s", command, String.join(" ", args)));

        try {
            var content = this.bufferedReader.readLine();

            // If the content returns as null, this means that the socket died...
            if (content == null) {
                throw new SocketTimeoutException("Socket timeout out.");
            }

            response = mapper.readTree(content);
        } catch (SocketException | SocketTimeoutException e) {
            // When the stream throws a SocketException, this means that server
            // unexpectedly severed our connection. This could mean that the server
            // died or close the connection. In this case, we'll report this to the
            // user and invoke a clean-up operation.
            System.out.printf("Couldn't send '%s' command. Lost Connection to %s:%s%n", command, this.host, this.port);
            this.cleanup();

            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
}

