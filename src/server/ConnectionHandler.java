package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import common.Configuration;
import common.DisconnectedException;
import common.resources.DirectoryEntry;
import common.resources.FileEntry;
import common.resources.IEntry;
import server.protocol.Command;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConnectionHandler extends Thread {
    public final static ObjectMapper mapper = new ObjectMapper();

    private final Configuration config = Configuration.getInstance();


    private final Socket connection;
    private InputStream inputStream;
    private PrintWriter outputStream;
    private OutputStream fileOutputStream;
    private BufferedReader reader;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public ConnectionHandler(Socket connection) {
        this.connection = connection;

        try {
            this.inputStream = connection.getInputStream();
            this.fileOutputStream = connection.getOutputStream();
            this.outputStream = new PrintWriter(connection.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(this.inputStream));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void run() {
        this.running.set(true);

        try {
            this.listen();
        } catch (IOException | DisconnectedException e) {
            this.cleanup();
        }
    }

    public void shutdown() {
        this.running.set(false);
    }

    private void listen() throws DisconnectedException, IOException {
        while (this.running.get()) {

            // We expect to get the first argument as the name of the command that is being
            // invoked for the server to respond. Any further components of the request are
            // treated as arguments that complement the command. For example, if the requester
            // send the request "Get file_a.txt", then the server should perform the get
            // command with the file argument as 'file_a.txt'.
            var request = reader.readLine().split(" ");
            var command = Command.valueOf(request[0]);

            // if readLine fails we can deduce here that the connection to the client is broken
            // and shut down the connection on this side cleanly by throwing a common.DisconnectedException
            // which will be passed up the call stack to the nearest handler (catch block)
            // in the run method
            if (command == null) {
                throw new DisconnectedException(" ... client has closed the connection ... ");
            }

            // create an initial json object that will be used as a response.
            var response = mapper.createObjectNode();

            switch (command) {
                case List: {
                    var fileList = mapper.createArrayNode();

                    // The requester can specify if they want to list the contents
                    // of a specific folder which is located under the upload folder,
                    // hence if they just want to list the root, then we'll set the
                    // additional argument to nothing.
                    var listArg = request.length > 1 ? request[1] : "";

                    // Iterate over the entry list and append the appropriate metadata on
                    for (IEntry entry : this.getUploadFolderContents(listArg)) {
                        var fileEntry = mapper.createObjectNode();


                        // append metadata about the objects in the
                        fileEntry.put("type", entry.getType().toString());
                        fileEntry.put("path", entry.getPath().getFileName().toString());

                        fileList.add(fileEntry);
                    }

                    // set the data into the response.
                    response.set("files", fileList);
                    break;
                }
                case Peers: {
                    response.put("message", "Peers not implemented yet.");
                    break;
                }
                case Get: {
                    response = getFileMetadata(request[1]);
                    break;
                }
                case Download: {
                    response = getFileMetadata(request[1]);


                    if (response.get("status").asBoolean()) {
                        var file = new File(String.valueOf(Paths.get(config.get("upload"), request[1])));

                        String encodedFile = Base64.getEncoder().encodeToString(this.loadFile(file.toPath()));

                        outputStream.write(encodedFile);

                        // skip writing the response object to outputStream since we're only
                        // responding with the file.
                        continue;
                    }
                }
            }

            // Finally, convert the response into a byte array and send it to the client.
            outputStream.println(mapper.writeValueAsString(response));
        }

        // Invoke the clean-up function after the listener finishes it's work, or gets
        // terminated externally by the server.
        this.cleanup();
    }

    private ObjectNode getFileMetadata(String filePath) {
        ObjectNode response = mapper.createObjectNode();

        // the requester didn't provide an argument to the get command, and
        // hence is asking to get nothing.
        if (filePath == null) {
            response.put("message", "Nothing to get.");
            response.put("status", false);

            return response;
        }

        // test that the fileURI is valid relative to our upload folder.
        // We must prevent the client from attempting to query a file out
        // of the upload folder scope. We will attempt to concatenate the
        // provided fileURI with our upload folder value. If the path
        // exists and is a file
        var file = new File(String.valueOf(Paths.get(config.get("upload"), filePath)));

        if (!file.getAbsolutePath().startsWith(config.get("upload")) || !file.exists()) {
            response.put("message", "No such file exists.");
            response.put("status", false);

            return response;
        }

        // compute the size and md5 hash of the file, send the parameters to
        // the requester as specified by the protocol...
        var fileLoader = new FileEntry(Path.of(file.getAbsolutePath()));

        response.put("digest", fileLoader.getDigest());
        response.put("size", fileLoader.getSize());
        response.put("message", "Getting file...");
        response.put("status", true);

        return response;
    }


    private byte[] loadFile(Path fileName) {
        var loader = new FileEntry(fileName);
        loader.load();

        var fileStream = new BufferedInputStream(loader.getFileStream());

        // @Cleanup: potential unsafe cast, what if the file is larger than 2GB
        byte[] buffer = new byte[(int) loader.getSize()];

        try {
            fileStream.read(buffer, 0, buffer.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer;
    }

    /**
     * Method to list the contents of the upload folder. This method will cycle through
     * all of the upload folder contents and convert each entry into either a DirectoryEntry
     * or FileEntry. FileEntry object has several methods which allows the caller to invoke
     * methods that can retrieve metadata from the file like the size or compute the md5 hash.
     *
     * @return A list of file/directory entries based on the location of the upload folder.
     */
    private List<IEntry> getUploadFolderContents(String folderName) throws FileNotFoundException {
        File uploadFolder = Paths.get(Configuration.getInstance().get("upload"), folderName).toFile();

        if (uploadFolder.isFile()) {
            throw new IllegalArgumentException("Upload folder must not be a file.");
        }

        if (!uploadFolder.getAbsolutePath().startsWith(config.get("upload")) || !uploadFolder.exists()) {
            throw new FileNotFoundException("no such folder exists.");
        }


        List<IEntry> files = new ArrayList<>();

        // Loop through each entry in the upload folder and convert them into FileEntry objects
        for (File file : uploadFolder.listFiles()) {
            if (file.isFile()) {
                files.add(new FileEntry(file.toPath()));
            } else {
                files.add(new DirectoryEntry(file.toPath()));
            }
        }

        return files;
    }


    private void cleanup() {
        try {
            reader.close();
            inputStream.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonNode sendCommand(Command command) {
        var response = mapper.createObjectNode();

        // TODO: implement sending a command.

        return response;
    }
}
