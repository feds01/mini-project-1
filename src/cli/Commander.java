package cli;

import cli.printers.ResourceTable;
import client.Client;
import client.Downloader;
import client.DownloaderStatus;
import common.Configuration;
import common.Networking;
import common.protocol.Command;
import server.PeerRecord;
import server.Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    Map<String, PeerRecord> knownPeers = new ConcurrentHashMap<>();

    /**
     * Variable that holds the client object which commander uses to send
     * commands and receive data from the peer.
     */
    private Client client;

    /**
     * Loaded help text from internal resource.
     */
    private String helpText;

    /**
     * Instance of the configuration object which is used to get application settings.
     */
    private final Configuration config = Configuration.getInstance();

    /**
     * This is an internal list of download instances that are being orchestrated by the server
     */
    private final Map<String, List<Downloader>> downloadMap = new HashMap<>();

    /**
     * Variable that holds the reference of this object that is used
     * when external callers need to access the commander.
     */
    private static final Commander instance = new Commander();

    /**
     * Commander instantiation method.
     */
    private Commander() {
        try {
            var resourceUri = this.getClass().getClassLoader().getResource("resources/help.txt");

            // load in the help text that is stored in a resource file called 'help.txt'
            if (resourceUri != null) {
                this.helpText = new String(Files.readAllBytes(Paths.get(resourceUri.toURI())));
            } else {
                throw new FileNotFoundException("Couldn't load crucial resources.");
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("Couldn't load crucial resources.");
            this.helpText = "Missing resource.";
        }
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
        var self = new PeerRecord(localAddress, addr.getHostName(), true);
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
                    this.addKnownPeer(new PeerRecord(command[1], this.client.getHost(), true));

                    // Get ourselves from the peer list and send the item to the newly established connection
                    // so that they know about this peer being present on the network
                    var selfPeer = this.knownPeers.values()
                            .stream()
                            .filter(PeerRecord::isSelf)
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

                    var clientDownloads = this.downloadMap.get(this.client.getAddress());

                    // Check whether we have any ongoing downloads on the current client connection
                    if (clientDownloads != null && clientDownloads.size() > 0) {
                        boolean isBeingDownloaded = false;


                        // if the hash and path of the resource matches the current GetMeta request, we'll abort
                        // this request and notify the user that they are already downloading the resource
                        for (var download : clientDownloads) {
                            if (download.getPath().equals(response.get("path").asText())) {
                                isBeingDownloaded = true;
                                break;
                            }
                        }

                        if (isBeingDownloaded) {
                            return "Resource already being downloaded. Use 'status' to check progress";
                        }
                    }

                    // we need to check that the partition or disk that the download folder
                    // is present on has enough free space (initially) to save the file.
                    // Otherwise, we won't be able to write the file onto the storage.
                    try {
                        var downloadPath = Downloader.getPathForResource(response.get("fileName").asText());

                        // Ensure that the download folder exists.
                        if (!downloadPath.getParent().toFile().exists()) {
                            return "Download folder doesn't exist. Aborting download!";
                        }

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
                        this.downloadMap.putIfAbsent(this.client.getAddress(), new ArrayList<>());
                        this.downloadMap.get(this.client.getAddress()).add(downloader);
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
                if (this.downloadMap.size() == 0) {
                    return "No active downloads.";
                }

                for (var host : this.downloadMap.keySet()) {
                    var completedDownloads = new ArrayList<Downloader>();

                    for (var download : this.downloadMap.get(host)) {
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

                    this.downloadMap.get(host).removeAll(completedDownloads);

                    // Remove the host entry if all of the downloads on this host finished.
                    if (this.downloadMap.get(host).size() == 0) {
                        this.downloadMap.remove(host);
                    }
                }

                break;
            }
            case "help": {
                // print out help string.
                return this.helpText;
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
     * Method that is used to add a new Peer to the knownPeer list. Put the
     * peer connection, or put if the Peer object mismatches with the current
     * record. This is because other peers may notify us that some of their
     * knownPeer connections have dropped or timed out.
     *
     * @param peer - The peer that will be added to the knownPeer list
     */
    public void addKnownPeer(PeerRecord peer) {
        var currentPeer = this.knownPeers.get(peer.getAddress());

        // Only put the peer record if there is no present record or if the PeerRecord differ.
        if (currentPeer == null || !currentPeer.equals(peer)) {
            this.knownPeers.put(peer.getAddress(), peer);
        }
    }


    /**
     * Method to get the active ongoing downloads
     *
     * @return A list of Downloader objects
     */
    public Map<String, List<Downloader>> getDownloadMap() {
        return this.downloadMap;
    }


    /**
     * Method to get the stored knownPeers
     *
     * @return A map that maps an IP address to a Peer object
     */
    public Map<String, PeerRecord> getKnownPeers() {
        return this.knownPeers;
    }
}
