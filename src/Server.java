import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server implements Runnable {
    private final int port;
    private Thread worker;
    private ServerSocket serverSocket;
    private ConnectionHandler connection;
    private final CountDownLatch startSignal;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public Server(int port) {
        this.port = port;
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
        Thread.currentThread().setName("Server");

        try {
            this.running.set(true);

            // listen for client connection requests on this server socket
            this.serverSocket = new ServerSocket(port);

            System.out.printf("File server listening on %s...%n", port);
            this.startSignal.countDown();

            while (this.running.get()) {
                Socket socket = this.serverSocket.accept();

                System.out.printf("Got new request from %s%n", socket.getInetAddress());

                var commander = Commander.getInstance();
                boolean prompt;

                // invoke prompt to accept if we're gonna work with this new connection.
                if (this.connection != null) { // TODO: check if we're transferring a file or resource.
                    prompt = commander.pushCommand("connect", "prompt", "priority");
                } else {
                    prompt = commander.pushCommand("connect", "prompt");
                }

                if (prompt) {
                    this.createConnection(socket);
                } else {
                    var outStream = new DataOutputStream(socket.getOutputStream());

                    outStream.writeUTF(String.format("%s: Client denied connection.", ServerStatus.DENY_CONNECTION));
                    socket.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createConnection(Socket connection) {
        // create new handler for this connection
        this.connection = new ConnectionHandler(connection);
        this.connection.start();
    }


    public void cleanup() {
        try {
            // close the connection if one exists...
            if (connection != null && connection.isAlive()) {
                connection.shutdown();
            }

            System.out.println("File server shutting down...");
            serverSocket.close();

        } catch (IOException e) {
            System.out.println("File server couldn't shutdown gracefully.");
            e.printStackTrace();
        }
    }
}