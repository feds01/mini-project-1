package cli;

import client.Client;
import common.Networking;
import com.fasterxml.jackson.core.JsonProcessingException;
import common.Configuration;
import server.Server;

public class Commander {
    private Server server;
    private Client client;

    private static final Commander instance = new Commander();

    private Commander() {
    }

    public static Commander getInstance() {
        return instance;
    }


    public void start() {
        System.out.print("> ");
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String pushCommand(String commandString) {

        // check if the given command is empty or just whitespaces, if so skip
        // attempting to decipher the given command.
        if (commandString.isBlank() || commandString.isEmpty()) {
            return "Command not recognised.";
        }

        var command = commandString.split(" ");

        switch (command[0]) {
            case "connect": {
                // expect one additional parameter, being the address
                if (command.length != 2) {
                    return "Usage: connect <address>";
                }

                try {
                    var address = Networking.parseAddressFromString(command[1]);

                    this.client = new Client(address.getHostName(), address.getPort());
                } catch (IllegalArgumentException e) {
                    return e.getMessage();
                }

                break;
            }
            case "quit": {
                server.stop();
                System.exit(0);
            }
            case "set": {

                // When the set command is invoked, it expects there to be an additional two
                // arguments specifying the property name, and it's new value.
                if (command.length != 3) {
                    return "Usage: set [download|upload] <path>";
                }

                try {
                    Configuration.getInstance().set(command[1], command[2]);
                    break;
                } catch (IllegalArgumentException e) {
                    return e.getMessage();
                }
            }

            case "list": {
                if (this.client == null) {
                    return "Not connected to any peer.";
                }

                var response = this.client.sendCommand("list");

                // Check to ensure that the client sent, and received a response. If something
                // didn't go to accord on the way, the response object should be a null. Hence,
                // it's ok to skip printing the response and to move on.
                if (response != null) {
                    // Use a printer function provided by resource list.
                    ResourceList.printResources(response);
                }

                break;
            }
            case "clear": {
                System.out.print("\033[H\033[2J");
                break;
            }
            case "search": {
                System.out.println("Searching for online peers...");
                break;
            }
            case "pull": {
                if (this.client == null) {
                    return "Not connected to any peer.";
                }

                // TODO: This is temporarily here for testing.
                var response = this.client.sendCommand("get");


                try {
                    return Client.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                break;
            }
            case "help": {
                // print out help stuff
                System.out.println("help text");
                break;
            }
            default: {
                return "Command not recognised.";
            }
        }

        return "";
    }
}
