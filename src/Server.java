import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
    private final int port;
    private ServerSocket serverSocket;
    private ConnectionHandler connection;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            // listen for client connection requests on this server socket
            this.serverSocket = new ServerSocket(port);

            System.out.printf("File server listening on %s...%n", port);
            while (true) {
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
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void createConnection(Socket connection) {
        // create new handler for this connection
        this.connection = new ConnectionHandler(connection);
        this.connection.start();
    }

    public void cleanup() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("File server couldn't shutdown gracefully...");
            e.printStackTrace();
        }
    }
}