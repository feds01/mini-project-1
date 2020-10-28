package server;


import cli.Commander;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.Configuration;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The server class used to accept new incoming connections and convert them into
 * connection handlers. The server is also responsible for sending out Multicast
 * messages to the local network to find any potential peer connections.
 *
 * @author 200008575
 */
public class Server implements Runnable {
    /**
     * The port number that the server socket will run on.
     */
    private final int port;

    /**
     * The multicast group that will be used for broadcasting requests for peers.
     */
    private InetAddress broadcastGroup;

    /**
     * The multicast socket that will be used for broadcasting requests for peers.
     */
    private DatagramSocket broadcastSocket;

    /**
     * Our reference to the commander object.
     */
    private final Commander commander = Commander.getInstance();


    /**
     * An instance of a Jackson ObjectMapper, used to serialize data that
     * will be transmitted through the multicast.
     */
    public final static ObjectMapper mapper = new ObjectMapper();


    /**
     * The thread instance that is used to run the server on.
     */
    private Thread worker;

    /**
     * Server socket instance that will be used to accept incoming TCP connections.
     */
    private ServerSocket serverSocket;

    /**
     * A concurrent safe counter used to determine if the server has started and is
     * listening to connections. If the value of the startSignal is zero, this means
     * that the server is ready.
     */
    private final CountDownLatch startSignal = new CountDownLatch(1);

    /**
     * Variable to hold the running status of the server. This variable must be atomic
     * since the server instance can be accessed within multiple threads.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Our scheduler instance that will broadcast a message along the local network looking
     * for any peers also listening to the broadcast.
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * A list of active connections that the server holds.
     */
    List<ConnectionHandler> connections = new ArrayList<>();

    /**
     * This is a Runnable task that is executed by the scheduler every 60 seconds to
     * check that all our 'active' connections aren't stale, and to broadcast a message
     * to the local network, looking for peers.
     */
    final Runnable broadcastTask = () -> {
        List<ConnectionHandler> deadConnections = new ArrayList<>();

        // iterate over the active connections and check if they are all still alive.
        // If some connections aren't alive, they are removed from the list since they
        // are no longer required.
        for (var connection : connections) {
            if (!connection.isAlive()) {
                deadConnections.add(connection);
            }
        }

        this.connections.removeAll(deadConnections);

        // Only attempt to broadcast if the Peer service started
        if (!broadcastSocket.isClosed()) {
            try {
                // Broadcast a message to the broadcast socket.
                var buffer = mapper.writeValueAsBytes(commander.getKnownPeers().values());

                var packet = new DatagramPacket(buffer, buffer.length, broadcastGroup, Configuration.MULTICAST_PORT);

                broadcastSocket.send(packet);
            } catch (IOException e) {
                // Couldn't send the packet across to the broadcast socket, close this socket
                // and don't attempt to send anything after.
                this.broadcastSocket.close();
            }
        }
    };

    /**
     * Server class constructor that instantiates the broadcast socket and group.
     */
    public Server(int port) {
        this.port = port;

        // Initiate our broadcast group address for finding peers on the local network.
        try {
            this.broadcastSocket = new DatagramSocket();
            this.broadcastGroup = InetAddress.getByName(Configuration.MULTICAST_GROUP);
        } catch (IOException e) {
            System.out.println("Failed to start Peer broadcasting service.");
        }
    }

    /**
     * Method to get the associated CountDownLatch in order to await for
     * the countdown to reach to zero, essentially acting as a lock.
     *
     * @return the server's start signal
     */
    public CountDownLatch getStartSignal() {
        return this.startSignal;
    }

    /**
     * Method to start the server
     */
    public void start() {
        worker = new Thread(this);
        worker.start();
    }

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
    public void stop() {
        running.set(false);
    }

    /**
     * Method that hosts the server socket listener which runs until the server is shutdown
     * or unexpectedly exits. Once the serverSocket is acquired, the countdown latch is
     * decremented to signal that the server has successfully started.
     *
     * The method also sets of our scheduler to run the broadcastTask every 60 seconds.
     */
    public void run() {
        Thread.currentThread().setName("ServerThread");

        // Add a scheduled task to query all of our known peers for their
        // peer information. This is also used to check if some connections
        // are dead instead of the ConnectionHandler notifying the server
        // instance that said connection is dead. By using a ScheduledExecutorService
        // task we can run a 'Runnable' instance every 60 seconds to send the
        // 'peers' command.
        var handle = scheduler.scheduleWithFixedDelay(broadcastTask, 2, 5, TimeUnit.SECONDS);

        scheduler.schedule((Runnable) () -> handle.cancel(true), 60, TimeUnit.SECONDS);


        try {
            this.running.set(true);

            // listen for client connection requests on this server socket
            this.serverSocket = new ServerSocket(port);



            System.out.printf("File server listening on port %s...%n", port);
            this.startSignal.countDown();

            while (this.running.get()) {
                var socket = this.serverSocket.accept();

                this.addConnection(socket);
            }
        } catch (IOException e) {
            if (e instanceof BindException) {
                System.out.printf("Port %s is in use, cannot start server.%n", port);
            } else {
                System.out.println("File server shutting down...");
            }
        } finally {
            this.cleanup();
        }
    }


    /**
     * Method to create a new connection when the server receives a new connection. The method
     * will use a given socket to instantiate a ConnectionHandler object, call 'start' to initiate
     * the connection and then add it to our connections list.
     *
     * @param socket The socket connection that is used to create the ConnectionHandler object
     */
    private void addConnection(Socket socket) {

        // create new handler for this connection
        try {
            var connection = new ConnectionHandler(socket);
            connection.start();

            this.connections.add(connection);
        } catch (IOException e) {
            // Ignore IOExceptions from creating a new ConnectionHandler
            // since we can just skip adding it at all.
        }
    }


    /**
     * Method to shutdown the server instance gracefully. This method will check if it needs to close
     * any current connections or transfers. The method will also close the SocketServer instance after
     * it finishes closing any connections it is currently hosting.
     */
    private void cleanup() {
        try {
            // check if we need to close any pending connections.
            this.connections.forEach(ConnectionHandler::stop);

            if (serverSocket != null) {
                serverSocket.close();
            }

            // close the broadcast socket...
            this.broadcastSocket.close();

        } catch (IOException e) {
            System.out.println("File server couldn't shutdown gracefully.");
            e.printStackTrace();
        } finally {
            worker.interrupt();
        }
    }

    /**
     * Method to get the port that the server is running on
     *
     * @return The port number
     * */
    public int getPort() {
        return this.port;
    }
}