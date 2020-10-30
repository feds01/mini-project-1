import cli.Commander;
import client.Downloader;
import common.Configuration;
import common.Networking;
import server.PeerReceiver;
import server.Server;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * The entry point of the program, this is where the client and server, and peer
 * listener component are started.
 *
 * @author 200008575
 */
public class FileShareMain {

    /**
     * Prefix that is used to display a new line in the command line interface.
     */
    static String CONSOLE_PREFIX = "> ";

    /**
     * The entry point method of the program
     */
    public static void main(String[] args) {
        var config = Configuration.getInstance();

        var port = Networking.getFreePort();

        var useBroadcast = true;

        try (
                var scanner = new Scanner(System.in)
        ) {
            if (args.length > 1) {
                port = Integer.parseInt(args[0]);
            }

            // Check for the -noBroadcast argument
            if (args.length > 2) {
                // @Improve: Add a proper arguments parse rather than doing crude
                // string comparisons. This approach also forces
                useBroadcast = !args[1].equals("-noBroadcast");
            }


            // boot the server that listens for incoming connections...
            var server = new Server(port, useBroadcast);

            server.start();


            // wait for the server to state that it's successfully started since. If the server
            // couldn't start after 1 second then we exit. we need to wait for the server to
            // start since we need to instantiate our Commander and PeerReceiver instances with
            // the server object...
            boolean hasStarted = server.getStartSignal().await(1, TimeUnit.SECONDS);

            // The server couldn't be started, and hence we can't perform any further operations
            if (!hasStarted) {
                System.exit(-1);
            }

            // print out application settings.
            System.out.println("Running with download directory: " + config.get("download"));
            System.out.println("Running with upload directory: " + config.get("upload"));

            // notify user that they can use 'help' command
            System.out.println("Use 'help' to print application commandline manual");

            // boot up PeerReceiver to listen for broadcasts of similar applications...
            var peerReceiver = new PeerReceiver();

            if (useBroadcast) {
                peerReceiver.start();
            }

            // boot up the commander for cli...
            var commander = Commander.getInstance();

            commander.setServer(server);
            commander.start();

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


            // Shutdown the PeerReceiver if it's still running...
            if (peerReceiver.isRunning()) {
                peerReceiver.shutdown();
            }

            // Clean-up any unfinished downloads that are currently active...
            commander.getDownloadMap().values().forEach(item -> item.forEach(Downloader::stop));
        } catch (NumberFormatException e) {
            System.out.println("Port argument must be an integer.");
            System.out.println("Usage: FileShareMain <port> [-noBroadcast]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
