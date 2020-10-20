package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.Configuration;
import common.DisconnectedException;
import server.resources.DirectoryEntry;
import server.resources.FileEntry;
import server.resources.IEntry;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHandler extends Thread {
    public final static ObjectMapper mapper = new ObjectMapper();

    private final Socket connection;
    private InputStream inputStream;
    private DataOutputStream outputStream;
    private BufferedReader reader;
    private boolean running;

    public ConnectionHandler(Socket connection) {
        this.connection = connection;

        try {
            this.inputStream = connection.getInputStream();
            this.outputStream = new DataOutputStream(connection.getOutputStream());
            this.reader = new BufferedReader(new InputStreamReader(this.inputStream));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void run() {
        this.running = true;

        try {
            this.listen();
        } catch (Exception e) { // exit cleanly for any Exception (including IOException, ClientDisconnectedException)
            System.out.println("server.server.ConnectionHandler:run " + e.getMessage());
        } finally {
            this.cleanup();
        }
    }

    public void shutdown() {
        this.running = false;
    }

    private void listen() throws DisconnectedException, IOException {
        while (this.running) {
            var line = reader.readLine();

            // if readLine fails we can deduce here that the connection to the client is broken
            // and shut down the connection on this side cleanly by throwing a common.DisconnectedException
            // which will be passed up the call stack to the nearest handler (catch block)
            // in the run method
            if (line == null || line.equals("null") || line.equals(Configuration.exitString)) {
                throw new DisconnectedException(" ... client has closed the connection ... ");
            }

            // create an initial json object that will be used as a response.
            var response = mapper.createObjectNode();

            switch(line) {
                case "list":
                    var fileList = mapper.createArrayNode();

                    // Iterate over the entry list and append the appropriate metadata on
                    for (IEntry entry : this.getUploadFolderContents()) {
                        var fileEntry = mapper.createObjectNode();


                        // append metadata about the objects in the
                        fileEntry.put("type", entry.getType().toString());
                        fileEntry.put("path", entry.getPath().toString());

                        fileList.add(fileEntry);
                    }

                    // set the data into the response.
                    response.set("files", fileList);
                    break;
                case "peers": {
                    response.put("message", "Peers not implemented yet.");
                    break;
                }
                case "get": {
                    response.put("message", "Get not implemented yet.");
                    break;
                }

                default: {
                    response.put("message", "Command is invalid.");
                }
            }

            // Finally, convert the response into a byte array and send it to the client.
            outputStream.write(mapper.writeValueAsBytes(response));
            outputStream.write('\r');
            outputStream.flush();
        }
    }

    /**
     * Method to list the contents of the upload folder. This method will cycle through
     * all of the upload folder contents and convert each entry into either a DirectoryEntry
     * or FileEntry. FileEntry object has several methods which allows the caller to invoke
     * methods that can retrieve metadata from the file like the size or compute the md5 hash.
     *
     * @return A list of file/directory entries based on the location of the upload folder.
     * */
    private List<IEntry> getUploadFolderContents() {
        File uploadFolder = new File(Configuration.getInstance().get("upload"));

        List<IEntry> files = new ArrayList<>();

        // Loop through each entry in the upload folder and convert them into FileEntry objects
        for (File file : uploadFolder.listFiles()) {
            if (file.isFile()) {
                files.add( new FileEntry(file.toPath()));
            } else {
                files.add( new DirectoryEntry(file.toPath()));
            }
        }

        return files;
    }


    private void cleanup() {
        System.out.println("server.server.ConnectionHandler: cleanup");
        try {
            reader.close();
            inputStream.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("server.server.ConnectionHandler: cleanup failed - " + e.getMessage());
        }
    }
}
