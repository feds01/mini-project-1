package cli;

import cli.printers.ResourceTable;
import client.Client;
import client.Downloader;
import client.DownloaderStatus;
import common.Configuration;
import common.Networking;
import common.protocol.Command;
import server.Peer;
import server.Server;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class that is responsible for handling the command line
 * aspect of the application.
 *
 * @author 200008575
 */
public class Commander {
    /**
     * A reference to the server object
     */
    private Server server;

    /**
     * A Hash map of known peers on the network that is mapped by the address of
     * the peer to the peer object.
     */
    Map<String, Peer> knownPeers = new ConcurrentHashMap<>();

    /**
     * Variable that holds the client object which commander uses to send
     * commands and receive data from the peer.
     */
    private Client client;


    /**
     * Instance of the configuration object which is used to get application settings.
     */
    private final Configuration config = Configuration.getInstance();

    /**
     * This is an internal list of download instances that are being orchestrated by the server
     */
    private final List<Downloader> downloads = new ArrayList<>();

    /**
     * Variable that holds the reference of this object that is used
     * when external callers need to access the commander.
     */
    private static final Commander instance = new Commander();

    /**
     * Commander instantiation method.
     */
    private Commander() {
    }

    /**
     * Method to get an instance of the Commander object
     *
     * @return A reference of this object.
     */
    public static Commander getInstance() {
        return instance;
    }


    /**
     * Method to start the commander. It will firstly add the local address of the
     * program to knownPeers, so that other member on the network can know about this
     * peer.
     *
     * @throws UnknownHostException if the method failed to acquire the local address of the machine
     */
    public void start() throws UnknownHostException {
        // Add ourselves to the knownPeers set so other can know that we exist on the network...
        var addr = InetAddress.getLocalHost();
        var localAddress = addr.getHostAddress() + ":" + server.getPort();

        // This is reference to our own instance
        var self = new Peer(localAddress, addr.getHostName(), true);
        this.knownPeers.put(localAddress, self);

        self.setSelf(true);

        // print the initial line of the cli
        System.out.print("> ");
    }

    /**
     * Method to set a reference to the server object.
     *
     * @param server - The reference to the server object
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Method to push a command to the Commander. The method will parse and
     * attempt to execute the associated action with the given command. If the
     * command is not recognised or if the command is improperly formed, the
     * method will return a string that denotes the error with the given command.
     * If the command is valid, an empty string is returned to denote that the
     * command is valid. This method may also print content to the console if it
     * is part of the action that is invoked by the command. For example if the
     * command 'list' was passed, this method will print the response from
     * the current connection (if exists) of the contents of the 'upload folder' on
     * other peer's side.
     *
     * @param commandString The command that will be parsed and interpreted by the
     *                      commander
     * @return A string denoting the error message (if any) with the command.
     */
    public String pushCommand(String commandString) {

        // check if the given command is empty or just whitespaces, if so skip
        // attempting to decipher the given command.
        if (commandString.isBlank() || commandString.isEmpty()) {
            return "Command not recognised.";
        }

        var command = commandString.split(" ");

        switch (command[0]) {
            case "join": {
                // expect one additional parameter, being the address
                if (command.length != 2) {
                    return "Usage: join <address>";
                }

                try {
                    var addr = Networking.parseAddressFromString(command[1]);

                    this.client = new Client(addr.getAddress().getHostAddress(), addr.getPort());

                    // If we successfully connect to the other peer, this means that we can add them
                    // as a known peer for other peers to know about them
                    this.addKnownPeer(new Peer(command[1], this.client.getHost(), true));

                    // Get ourselves from the peer list and send the item to the newly established connection
                    // so that they know about this peer being present on the network
                    var selfPeer = this.knownPeers.values()
                            .stream()
                            .filter(Peer::isSelf)
                            .findFirst()
                            .get()
                            .getAddress();

                    this.client.sendCommand(Command.AddPeer, selfPeer);

                } catch (IllegalArgumentException e) {
                    // If the address is invalid, the message will be returned
                    return e.getMessage();
                } catch (UnknownHostException | ConnectException e) {
                    this.client = null;
                    return "Unknown host.";
                } catch (IOException e) {
                    this.client = null;
                    return "Couldn't establish connection with peer.";
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

                var response = this.client.sendCommand(Command.List, Arrays.copyOfRange(command, 1, command.length));

                // Check to ensure that the client sent, and received a response. If something
                // didn't go to accord on the way, the response object should be a null. Hence,
                // it's ok to skip printing the response and to move on.
                if (response != null && response.get("status").asBoolean()) {
                    var printer = new ResourceTable(response);

                    printer.print();  // Use a printer function provided by resource list.
                }
                if (response != null && !response.get("status").asBoolean()) {
                    // If the request failed to get metadata for any reason, we should notify the client
                    return response.get("message").asText();
                } else if (response == null) {
                    // Set this connection as a 'dead' connection in knownPeers
                    this.knownPeers.get(this.client.getAddress()).setAlive(false);
                }

                break;
            }
            case "peers": {
                this.knownPeers.values().forEach((item) -> System.out.println(item.toString()));
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
                var response = this.client.sendCommand(Command.GetMeta, Arrays.copyOfRange(command, 1, command.length));

                if (response != null && response.get("status").asBoolean()) {
                    var size = response.get("size").asLong();

                    // we need to check that the partition or disk that the download folder
                    // is present on has enough free space (initially) to save the file.
                    // Otherwise, we won't be able to write the file onto the storage.
                    try {
                        var downloadPath = Downloader.getPathForResource(response.get("fileName").asText());

                        // This is of course a very edge case scenario, but nevertheless a potential issue...
                        if (downloadPath.getParent().toFile().getFreeSpace() < size) {
                            return "Not enough space on download folder drive to download file.";
                        }

                        // spin up a downloader instance and start downloading the resource.
                        var downloader = new Downloader(
                                this.client.getHost(), this.client.getPort(),
                                downloadPath,
                                response
                        );

                        downloader.start();

                        // append the downloader thread to our downloader list
                        this.downloads.add(downloader);
                    } catch (InvalidPathException e) { // This is thrown when the download folder doesn't exist
                        return "Download folder doesn't exist. Aborting download!";
                    } catch (IOException e) {
                        return "Couldn't establish connection with peer.";
                    }
                } else if (response != null && !response.get("status").asBoolean()) {

                    // If the request failed to get metadata for any reason, we should notify the client
                    return response.get("message").asText();

                } else if (response == null) {
                    // Set this connection as a 'dead' connection in knownPeers
                    this.knownPeers.get(this.client.getAddress()).setAlive(false);
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

                    var status = download.getStatus();

                    // Remove the downloader instance from the list when it finished or failed
                    // because of a timeout, or just failed for some unknown I/O reason.
                    if (status.equals(DownloaderStatus.FINISHED) ||
                            status.equals(DownloaderStatus.FAILED) ||
                            status.equals(DownloaderStatus.FAILED_TIMEOUT)) {
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

        // If no errors occurred during the parsing of the given command, simply
        // return an empty string since there is nothing to print to the console.
        return "";
    }

    /**
     * Method that is used to add a new Peer to the knownPeer list
     *
     * @param peer - The peer that will be added to the knownPeer list
     */
    public void addKnownPeer(Peer peer) {
        this.knownPeers.putIfAbsent(peer.getAddress(), peer);
    }


    /**
     * Method to get the active ongoing downloads
     *
     * @return A list of Downloader objects
     */
    public List<Downloader> getDownloads() {
        return this.downloads;
    }


    /**
     * Method to get the stored knownPeers
     *
     * @return A map that maps an IP address to a Peer object
     */
    public Map<String, Peer> getKnownPeers() {
        return this.knownPeers;
    }
}
