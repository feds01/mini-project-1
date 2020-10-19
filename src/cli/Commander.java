package cli;

import common.Configuration;
import server.Server;

public class Commander {
    public Server server;

    private static final Commander instance = new Commander();

    private Commander() {
    }

    public static Commander getInstance() {
        return instance;
    }


    public void start() {
        System.out.print("> ");
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String pushCommand(String commandString) {

        // check if the given command is empty or just whitespaces, if so skip
        // attempting to decipher the given command.
        if (commandString.isBlank() || commandString.isEmpty()) {
            return "Command not recognised.";
        }

        var command = commandString.split(" ");

        switch (command[0]) {
            case "connect": {
                // expect 2 additional parameters

                if (command.length != 2) {
                    return "Usage: connect <address|dns>";
                }

                // connect code
                break;
            }
            case "quit": {
                server.stop();
                System.exit(0);
            }
            case "set": {

                // When the set command is invoked, it expects there to be an additional two
                // arguments specifying the property name, and it's new value.
                if (command.length != 3) {
                    return "Usage: set [download|upload] <path>";
                }

                try {
                    Configuration.getInstance().set(command[1], command[2]);
                    break;
                } catch (IllegalArgumentException e) {
                    return e.getMessage();
                }
            }

            case "list": {
                System.out.println("List files on peer's side.");
                break;
            }
            case "clear": {
                System.out.print("\033[H\033[2J");
                break;
            }
            case "search": {
                System.out.println("Searching for online peers...");
                break;
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
                return "Command not recognised.";
            }
        }

        return "";
    }
}
