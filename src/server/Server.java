package server;

import server.ConnectionHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
     * A list of ConnectionHandlers that the server shepherds over.
     * */
    Map<InetAddress, ConnectionHandler> connections;


    public Server(int port) {
        this.port = port;
        this.connections = new HashMap<>();
        this.startSignal = new CountDownLatch(1);
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
        this.cleanup();

        running.set(false);
        worker.interrupt();
    }

    public void run() {
        Thread.currentThread().setName("server.Server");

        try {
            this.running.set(true);

            // listen for client connection requests on this server socket
            this.serverSocket = new ServerSocket(port);

            System.out.printf("File server listening on %s...%n", port);
            this.startSignal.countDown();

            while (this.running.get()) {
                Socket socket = this.serverSocket.accept();

                this.addConnection(socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ConnectionHandler> getConnections() {
        return new ArrayList<>(this.connections.values());
    }


    public void addConnection(Socket socket) {

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
    public void cleanup() {
        try {
            // check if we need to close any pending connections.
            if (this.connections.size() > 0) {
                this.connections.values().forEach(ConnectionHandler::shutdown);
            }


            System.out.println("File server shutting down...");
            serverSocket.close();

        } catch (IOException e) {
            System.out.println("File server couldn't shutdown gracefully.");
            e.printStackTrace();
        }
    }
}