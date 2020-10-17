public class FileShareMain {
    public static void main(String[] args) {
        var config = Configuration.getInstance();

        if (args.length == 0) {
            System.out.println("Usage: java FileShareMain <port>");
            System.exit(1);
        }

        // print out application settings.
        System.out.println("Running with download directory: " + config.get("directory.download"));
        System.out.println("Running with upload directory: " + config.get("directory.upload"));

        // boot the server that listens for incoming connections...
        try {
            var server = new Server(Integer.parseInt(args[0]));

            server.start();

            // boot up the commander or gui version...

            // finally when client sends kill command, close the server and any other
            // resources that need closing...
            server.join();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
