import cli.Commander;
import client.Downloader;
import common.Networking;
import common.Configuration;
import server.Server;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 *
 * */
public class FileShareMain {

    static String CONSOLE_PREFIX = "> ";

    /**
     *
     * */
    public static void main(String[] args) {
        var config = Configuration.getInstance();

        var port = Networking.getFreePort();

        try (
                var scanner = new Scanner(System.in);
        ) {
            if (args.length == 1) {
                port = Integer.parseInt(args[0]);
            }


            var server = new Server(port);

            // boot the server that listens for incoming connections...
            server.start();


            // wait for the server to state that it's successfully started...
            boolean hasStarted = server.getStartSignal().await(1, TimeUnit.SECONDS);

            // The server couldn't be started, and hence we can't perform any operations
            if (!hasStarted) {
                System.exit(-1);
            }

            // print out application settings.
            System.out.println("Running with download directory: " + config.get("download"));
            System.out.println("Running with upload directory: " + config.get("upload"));

            // boot up the commander for cli...
            var commander = Commander.getInstance();

            commander.start();
            commander.setServer(server);

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

                // Begin the next line of input...
                System.out.print(CONSOLE_PREFIX);
            }

            // finally when client sends kill command, close the server and any other
            // resources that need closing...
            if (server.isRunning()) {
                server.stop();
            }

            // Clean-up any unfinished downloads that are currently active...
            commander.getDownloads().forEach(Downloader::cleanup);
        } catch (NumberFormatException e) {
            System.out.println("Port argument must be an integer.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
