package server;


import com.fasterxml.jackson.databind.JsonNode;
import server.protocol.Command;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements Runnable {
    private final int port;
    private Thread worker;
    private ServerSocket serverSocket;
    private final CountDownLatch startSignal;

    /**
     * Variable to hold the running status of the server. This variable must be atomic
     * since the server instance can be accessed within multiple threads.
     * */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Our scheduler instance that will query all of our 'known' peers for their
     * information on their known peers.
     * */
    private final ScheduledExecutorService scheduler;

    /**
     * A list of ConnectionHandlers that the server shepherds over.
     * */
    Map<InetAddress, ConnectionHandler> connections;

    final Runnable queryTask = new Runnable() {
        @Override
        public void run() {
            List<JsonNode> responses = new ArrayList<>();

            for (var connection : connections.values()) {
                var response = connection.sendCommand(Command.Peers);

                responses.add(response);
            }

            // TODO: do some logic with these responses.
        }
    };

    public Server(int port) {
        this.port = port;
        this.connections = new HashMap<>();
        this.startSignal = new CountDownLatch(1);

        this.scheduler = Executors.newScheduledThreadPool(1);

    }

    public CountDownLatch getStartSignal() {
        return this.startSignal;
    }

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public void stop() {
        running.set(false);
    }

    public void run() {
        Thread.currentThread().setName("server.Server");

        // Add a scheduled task to query all of our known peers for their
        // peer information. This is also used to check if some connections
        // are dead instead of the ConnectionHandler notifying the server
        // instance that said connection is dead. By using a ScheduledExecutorService
        // task we can run a 'Runnable' instance every 60 seconds to send the
        // 'peers' command.
        var handle = scheduler.scheduleAtFixedRate(queryTask, 2, 5, TimeUnit.SECONDS);

        scheduler.schedule((Runnable) () -> handle.cancel(true), 60, TimeUnit.SECONDS);


        try {
            this.running.set(true);

            // listen for client connection requests on this server socket
            this.serverSocket = new ServerSocket(port);

            System.out.printf("File server listening on 127.0.0.1:%s...%n", port);
            this.startSignal.countDown();

            while (this.running.get()) {
                var socket = this.serverSocket.accept();

                this.addConnection(socket);
            }
        } catch (IOException e) {
            System.out.println("File server shutting down...");
        } finally {
            this.cleanup();
        }
    }

    public List<ConnectionHandler> getConnections() {
        return new ArrayList<>(this.connections.values());
    }


    private void addConnection(Socket socket) {

        // create new handler for this connection
        var connection = new ConnectionHandler(socket);
        connection.start();

        this.connections.put(socket.getInetAddress(), connection);
    }


    /**
     * Method to shutdown the server instance gracefully. This method will
     * check if it needs to close any current connections or transfers. The
     * method will also close the SocketServer instance after it finishes closing
     * any connections it is currently hosting.
     *
     * If the server couldn't gracefully shutdown, the exception stack
     * trace is printed since this should be the exit point of the application.
     * */
    private void cleanup() {
        try {
            // check if we need to close any pending connections.
            if (this.connections.size() > 0) {
                this.connections.values().forEach(ConnectionHandler::shutdown);
            }

            serverSocket.close();

        } catch (IOException e) {
            System.out.println("File server couldn't shutdown gracefully.");
            e.printStackTrace();
        } finally {
            worker.interrupt();
        }
    }
}