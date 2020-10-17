import java.util.Scanner;

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

        try (
                var scanner = new Scanner(System.in);
        ) {
            var server = new Server(Integer.parseInt(args[0]));

            // boot the server that listens for incoming connections...
            server.start();

            server.getStartSignal().await();


            // boot up the commander or gui version...
            var commander = Commander.getInstance();

            commander.setServer(server);
            System.out.print("> ");

            while (scanner.hasNextLine()) {
                var command = scanner.nextLine().strip().split(" ");
                // TODO: don't strip and then validate

                // TODO: move this into Commander class.
                switch (command[0]) {
                    case "connect": {
                        // connect code
                        break;
                    }
                    case "quit": {
                        server.stop();
                        System.exit(0);
                    }
                    case "list": {
                        System.out.println("List files on peer's side.");
                        break;
                    }
                    case "search": {
                        System.out.println("Searching for online peers...");
                    }
                    case "pull": {
                        System.out.println("Pulling file....");
                        break;
                    }
                    case "help": {
                        // print out help stuff
                        System.out.println("help text");
                        break;
                    }
                    default: {
                        System.out.println("Command not recognised.");
                    }
                }

                System.out.print("> ");
            }


            // finally when client sends kill command, close the server and any other
            // resources that need closing...
            if (server.isRunning()) {
                server.stop();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
