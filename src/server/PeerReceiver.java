package server;

import cli.Commander;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import common.Configuration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * PeerReceiver class is a background thread that continuously listens for
 * multicast datagrams that transmit information about any other peers that
 * are present on the local network. If any peers are noted, they are added
 * to a map of knownPeers in the Commander instance.
 *
 * @author 200008575
 */
public class PeerReceiver extends Thread {
    /**
     * An instance of a Jackson ObjectMapper, used to deserialize data that
     * is transmitted through the multicast
     */
    public final static ObjectMapper mapper = new ObjectMapper();

    /**
     * Our reference to the commander object
     */
    private final Commander commander = Commander.getInstance();

    /**
     * The multicast group that will be used for broadcasting requests for peers.
     */
    private InetAddress broadcastGroup;

    /**
     * The multicast socket that will be used for broadcasting requests for peers.
     */
    private MulticastSocket broadcastSocket;

    /**
     * Variable to hold the running status of the server. This variable must be atomic
     * since the server instance can be accessed within multiple threads.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Buffer that's used to store the datagram packet data
     *
     * @implNote Consider that the packet size could be larger than this buffer size.
     */
    protected byte[] buffer = new byte[1024];

    /**
     * Method to get the running status of the server.
     *
     * @return a boolean signaling if the server is running or not.
     */
    public boolean isRunning() {
        return this.running.get();
    }

    /**
     * Method to stop the server
     */
    public void shutdown() {
        this.running.set(false);
    }


    /**
     * Method that is invoked when the Thread is started. It will continuously listen for
     * any incoming Datagram packets that are on the Multicast Group. If the packet is
     * received and valid, it is parsed into @see{Peer} objects and they are added to
     * the knownPeers map in commander.
     */
    @Override
    public void run() {
        Thread.currentThread().setName("PeerReceiverThread");
        this.running.set(true);

        try {
            // Setup the broadcast socket and group ready for receiving data on peers.
            this.broadcastSocket = new MulticastSocket(Configuration.MULTICAST_PORT);
            this.broadcastGroup = InetAddress.getByName(Configuration.MULTICAST_GROUP);

            this.broadcastSocket.joinGroup(this.broadcastGroup);

            // Run the listener until it is stopped externally
            while (this.running.get()) {
                var packet = new DatagramPacket(buffer, buffer.length);
                broadcastSocket.receive(packet);

                // Deserialize the sent over array of peer objects into a list of peer
                // objects that can be added into the list of known peers that the commander
                // object holds a reference of...
                try {
                    List<Peer> response = mapper.readValue(packet.getData(), new TypeReference<>() {
                    });

                    response.forEach(commander::addKnownPeer);
                } catch (MismatchedInputException e) {
                    // Ignore the error since we can just skip that packet.
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.cleanup();
        }
    }

    /**
     * Method that is used to close any open resources that PeerReceiver opened during
     * it's runtime.
     */
    public void cleanup() {
        try {
            this.broadcastSocket.leaveGroup(broadcastGroup);
            this.broadcastSocket.close();
        } catch (IOException e) {
            System.out.println("Peer listener couldn't finish gracefully.");
        }
    }
}
