package cli;

import client.Client;
import client.Downloader;
import com.fasterxml.jackson.core.JsonProcessingException;
import common.Configuration;
import common.Networking;
import server.Server;
import server.protocol.Command;

import java.util.ArrayList;
import java.util.List;

public class Commander {
    private Server server;
    private Client client;

    // This is an internal list of download instances that are being
    // orchestrated by
    private final List<Downloader> downloads = new ArrayList<>();

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


                var response = this.client.sendCommand(Command.List);

                // Check to ensure that the client sent, and received a response. If something
                // didn't go to accord on the way, the response object should be a null. Hence,
                // it's ok to skip printing the response and to move on.
                if (response != null) {
                    var printer = new ResourceList(response);

                    printer.print();  // Use a printer function provided by resource list.
                }

                break;
            }
            case "search": {
                System.out.println("Searching for online peers...");
                break;
            }
            case "get-file": {
                if (this.client == null) {
                    return "Not connected to any peer.";
                }

                // We'll first need to query the metadata on this file from the server.
                // We need to get the size of the file to check that it will fit onto
                // our local machine, and we need to get the computed MD5 hash so we can
                // later verify the integrity of the file...
                var response = this.client.sendCommand(Command.Get);

                // TODO: append the request to our Downloads...

                try {
                    return Client.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                break;
            }

            // Command to print the working status of any on-going downloads that are occurring.
            case "status": {
                if (this.downloads.size() == 0) {
                    return "No active downloads.";
                }

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
