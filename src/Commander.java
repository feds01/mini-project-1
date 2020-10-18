import java.util.List;
import java.util.Scanner;

public class Commander {
    public Server server;

    static final List<String> POLAR_OPTIONS = List.of("y", "n");

    private static final Commander instance = new Commander();

    private Commander() {
    }

    public static Commander getInstance() {
        return instance;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public String promptUser(String question, List<String> options) {
        var scanner = new Scanner(System.in);
        String choice;

        do {
            System.out.print(question);
            choice = scanner.nextLine();

        }  while (!options.contains(choice));

        return choice;
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
                System.out.print("\r");
                System.out.flush();
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
