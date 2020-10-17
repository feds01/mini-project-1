import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Commander {
    public Server server;

    private static final Commander instance = new Commander();

    private Commander() {
    }

    public static Commander getInstance() {
        return instance;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public boolean pushCommand(String command, String... args) {

        // build List from command and arguments...
        List<String> commandWithArgs = new ArrayList<>(List.of(command));

        commandWithArgs.addAll(Arrays.asList(args));


        Scanner scanner = new Scanner(String.join(" ", commandWithArgs));

        while (scanner.hasNext()) {
            String element = scanner.next().toLowerCase();

            System.out.println(element);
        }

        return true;
    }
}
