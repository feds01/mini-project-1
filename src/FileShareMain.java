import java.util.Scanner;

public class FileShareMain {
    public static void main(String[] args) {
        var config = Configuration.getInstance();

        if (args.length == 0) {
            System.out.println("Usage: java FileShareMain <port>");
            System.exit(1);
        }

        // print out application settings.
        System.out.println("Running with download directory: " + config.get("download"));
        System.out.println("Running with upload directory: " + config.get("upload"));

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
                var commandString = scanner.nextLine();

                // check if the given command is empty or just whitespaces, if so skip
                // attempting to decipher the given command.
                var commandResult = commander.pushCommand(commandString);

                // Don't print the result of the command if it's empty. If the result is empty,
                // the command finished gracefully.
                if (!commandResult.isEmpty()) {
                    System.out.println(commandResult);
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
