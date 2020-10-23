package cli;

import cli.printers.ResourceList;
import client.Client;
import client.Downloader;
import client.DownloaderStatus;
import common.Configuration;
import common.Networking;
import server.Server;
import common.protocol.Command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commander {
    private Server server;
    private Client client;

    private final Configuration config = Configuration.getInstance();

    // This is an internal list of download instances that are being
    // orchestrated by the server
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
                    this.config.set(command[1], command[2]);
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
                break;
            }
            case "get": {
                if (this.client == null) {
                    return "Not connected to any peer.";
                }

                // We'll first need to query the metadata on this file from the server.
                // We need to get the size of the file to check that it will fit onto
                // our local machine, and we need to get the computed MD5 hash so we can
                // later verify the integrity of the file...
                var response = this.client.sendCommand(Command.Get, Arrays.copyOfRange(command, 1, command.length));

                if (response.get("status").asBoolean()) {
                    var size = response.get("size").asLong();

                    // we need to check that the partition or disk that the download folder
                    // is present on has enough free space (initially) to save the file.
                    // Otherwise, we won't be able to write the file onto the storage.
                    var downloadPath = new File(config.get("download"));

                    // This is of course a very edge case scenario, but nevertheless a potential issue...
                    if (downloadPath.getFreeSpace() < size) {
                        return "No enough space on download folder drive to download file.";
                    }

                    // spin up a downloader instance and start downloading the resource.
                    var downloader = new Downloader(
                            this.client.getHost(), this.client.getPort(),
                            response
                    );

                    downloader.start();

                    // append the downloader thread to our downloader list
                    this.downloads.add(downloader);

                    break;
                } else {
                    // the response always returns the status of the request that can be used to
                    // inform the state of the request.
                    System.out.println(response.get("message").asText());
                }


                break;
            }
            // Command to print the working status of any on-going downloads that are occurring.
            case "status": {
                if (this.downloads.size() == 0) {
                    return "No active downloads.";
                }

                var completedDownloads = new ArrayList<Downloader>();

                for (var download : this.downloads) {
                    System.out.println(download.getProgressString());

                    // Remove the downloader instance from the list when it finished
                    // executing.
                    if (download.getStatus().equals(DownloaderStatus.FINISHED)) {
                        completedDownloads.add(download);
                    }
                }

                this.downloads.removeAll(completedDownloads);

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
