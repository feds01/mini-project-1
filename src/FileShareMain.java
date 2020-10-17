public class FileShareMain {
    public static void main(String[] args) {
        var config = ConfigurationManager.getInstance();

        // print out application settings.
        System.out.println(config.get("directory.download"));
        System.out.println(config.get("directory.upload"));



        if (args.length < 1) {
            System.out.println("Usage: FileShareMain <server|client> <hostname>");
            System.exit(1);
        }

        switch (args[0]) {
            case "client":
                Client c = new Client(args[1], Configuration.defaultPort);
                break;
            case "server":
                Server s = new Server(Configuration.defaultPort);
                break;
        }
    }
}
