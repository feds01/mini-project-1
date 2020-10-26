import client.Client;

public class RunClient {
    public static void main(String[] args) {
        try {
            Client client = new Client("localhost", 22017);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
