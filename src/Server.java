import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public Server(int port) {
        try {
            // listen for client connection requests on this server socket
            ServerSocket ss = new ServerSocket(port);

            System.out.println("Server started ... listening on port " + port + " ...");
            while (true) {
                Socket conn = ss.accept(); // will wait until client requests a connection, then returns connection (socket)
                System.out.println("Server got new connection request from " + conn.getInetAddress());
                ConnectionHandler ch = new ConnectionHandler(conn); // create new handler for this connection
                ch.start();                                         // start handler thread
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}