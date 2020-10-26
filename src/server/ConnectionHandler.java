package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import common.BaseConnection;
import common.Configuration;
import common.protocol.Command;
import common.resources.DirectoryEntry;
import common.resources.FileEntry;
import interfaces.IEntry;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * ConnectionHandler class is used by the server instance to handle
 * an incoming connection in it's own process. This means that the server
 * can handle multiple connections simultaneously making it more usable.
 *
 * @author 200008575
 */
public class ConnectionHandler extends BaseConnection implements Runnable {
    /**
     * An instance of a Jackson ObjectMapper, used to serialize data that
     * will be transmitted through the socket to the peer in order to transmit
     * responses.
     */
    public final static ObjectMapper mapper = new ObjectMapper();

    /**
     * Instance of the configuration object which is used to get application settings.
     */
    private final Configuration config = Configuration.getInstance();

    /**
     * The thread instance that is used to run the downloader instance on.
     */
    private Thread worker;

    /**
     * Variable to hold the running status of the ConnectionHandler instance. This
     * is atomic due to that the ConnectionHandler could be stopped within the server
     * when it shuts down. To ensure concurrent safety, it is made atomic so that
     * only one caller can set the running status of the ConnectionHandler.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructor method for the ConnectionHandler class.
     */
    public ConnectionHandler(Socket socket) throws IOException {
        super(socket);
    }

    /**
     * Method that is used to instantiate the ConnectionHandler. The listen
     * method is called with a exception handler wrapper that will invoke
     * the 'cleanup' method if the connection is unexpectedly stopped.
     */
    @Override
    public synchronized void run() {
        this.running.set(true);

        try {
            this.listen();
        } catch (Exception e) {
            this.cleanup();

            // If the socket timed out, stop the thread...
            if (e instanceof SocketTimeoutException) {
                this.stop();
            }
        }
    }

    /**
     * Method to start the ConnectionHandler.
     */
    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    /**
     * Method to return if the worker is still alive
     */
    public boolean isAlive() {
        return this.worker.isAlive();
    }

    /**
     * Method that is used to invoke a shutdown action of the ConnectionHandler. It
     * will set the running status of the thread to false, and then interrupt the
     * thread.
     */
    public void stop() {
        running.set(false);
        worker.interrupt();
    }

    /**
     * Method that acts as a listener for any requests from the peer. The method
     * will attempt to process the given command. When the command is processed, a response
     * object is formed in the form of an Object node that will written as a byte array to
     * the socket output stream (PrinterWriter) in the form of a line. However, if the request
     * is asking to download a resource, the byte array is written to a DataOutputStream
     * and no response object is returned.
     * <p>
     * If the command is not valid or isn't part of the transmission protocol, a response is still
     * returned to notify the peer that the request was invalid.
     *
     * @throws IOException if the peer connection drops whilst writing to an output stream.
     */
    private void listen() throws IOException {
        while (this.running.get()) {
            // We expect to get the first argument as the name of the command that is being
            // invoked for the server to respond. Any further components of the request are
            // treated as arguments that complement the command. For example, if the requester
            // send the request "Get file_a.txt", then the server should perform the get
            // command with the file argument as 'file_a.txt'.
            var request = this.bufferedReader.readLine().split(" ");
            var command = Command.valueOf(request[0]);

            // create an initial json object that will be used as a response.
            var response = mapper.createObjectNode();

            switch (command) {
                case List: {
                    var fileList = mapper.createArrayNode();

                    // The requester can specify if they want to list the contents
                    // of a specific folder which is located under the upload folder,
                    // hence if they just want to list the root, then we'll set the
                    // additional argument to nothing.
                    var listArg = "";

                    if (request.length > 1) {
                        listArg =  String.join(" ", Arrays.copyOfRange(request, 1, request.length));
                    }

                    // Iterate over the entry list and append the appropriate metadata on
                    try {
                        for (IEntry entry : this.getUploadFolderContents(listArg)) {
                            var fileEntry = mapper.createObjectNode();

                            // append metadata about the objects in the
                            fileEntry.put("type", entry.getType());
                            fileEntry.put("path", entry.getFileName());

                            fileList.add(fileEntry);
                        }

                        // set the data into the response.
                        response.set("files", fileList);
                        response.put("status", true);
                    } catch (IOException e) {
                        response.put("status", false);
                        response.put("message", "Folder not found");
                    } catch (IllegalArgumentException e) {
                        response.put("status", false);
                        response.put("message", "Path must a file");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    break;
                }
                case GetMeta:
                case Get: {
                    String relativeFilePath;

                    // Join all of the following arguments after 'Get/GetMeta' to allow for spaces in filenames.
                    if (request.length > 1) {
                        relativeFilePath = String.join(" ", Arrays.copyOfRange(request, 1, request.length));
                    } else {
                        // the requester didn't provide an argument to the get command, and
                        // hence is asking to get nothing.
                        response.put("message", "Nothing to get.");
                        response.put("status", false);

                        break;
                    }

                    FileEntry resource;

                    try {
                        resource = new FileEntry(Paths.get(config.get("upload"), relativeFilePath));
                    } catch (IllegalArgumentException e) {
                        response.put("message", "No such file exists.");
                        response.put("status", false);
                        break;
                    }

                    response = getFileMetadata(resource);

                    // Don't continue by downloading the file if it's only a metadata request, and don't
                    // continue if fetching the metadata failed for some reason.
                    if (command == Command.GetMeta || !response.get("status").asBoolean()) {
                        break;
                    }

                    var out = new DataOutputStream(this.socket.getOutputStream());
                    var fileBuffer = resource.getFileBuffer();

                    // write the file buffer to the DataOutputStream, flush it and immediately
                    // close it since we aren't going to need to use it anymore.
                    out.write(fileBuffer, 0, fileBuffer.length);
                    out.flush();
                    out.close();
                }
            }

            // Finally, convert the response into a byte array and send it to the client.
            this.printWriter.println(mapper.writeValueAsString(response));
        }

        // Invoke the clean-up function after the listener finishes it's work, or gets
        // terminated externally by the server.
        this.cleanup();
    }


    /**
     * Method to collect metadata on the given file such as file size, checksum and the
     * file name. If the file doesn't exist, not a file, or is not a child of the 'upload'
     * folder; the method will return an object with a message corresponding to the error.
     * The constructed object also holds a status of if the 'request' was valid or not.
     *
     * @param fileEntry - The file entry that the data should be converted into a JSON node
     * @return An ObjectNode that represents the metadata of the given file
     */
    private ObjectNode getFileMetadata(FileEntry fileEntry) {
        ObjectNode response = mapper.createObjectNode();

        // test that the fileURI is valid relative to our upload folder.
        // We must prevent the client from attempting to query a file out
        // of the upload folder scope. We will attempt to concatenate the
        // provided fileURI with our upload folder value. If the path
        // exists and is a file
        try {
            fileEntry.load();

            var file = fileEntry.getPath().toFile();

            if (!file.getAbsolutePath().startsWith(config.get("upload")) || !file.exists()) {
                response.put("message", "No such file exists.");
                response.put("status", false);

                return response;
            }

            // Serialize the FileEntry object, add a status to denote that the
            // request was successful.
            response = mapper.valueToTree(fileEntry);
            response.put("status", true);


        } catch (InvalidPathException e) {
            response.put("status", false);
            response.put("message", "Invalid file path");
        } catch (FileNotFoundException e) {
            response.put("status", false);
            response.put("message", "Couldn't access file on remote.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * Method to list the contents of the upload folder. This method will cycle through
     * all of the upload folder contents and convert each entry into either a DirectoryEntry
     * or FileEntry. FileEntry object has several methods which allows the caller to invoke
     * methods that can retrieve metadata from the file like the size or compute the md5 hash.
     *
     * @param folderName - The relative path to a folder within the 'upload' folder that will be
     *                   concatenated with the upload folder path to list the contents of the
     *                   child directory.
     * @return A list of file/directory entries based on the location of the upload folder.
     * @throws IllegalArgumentException if the formed path is a file and not a folder.
     * @throws FileNotFoundException    if the result of combining the 'upload' folder path and
     *                                  and the folderName parameter does not exist, or is not a
     *                                  child of the 'upload' folder.
     */
    private List<IEntry> getUploadFolderContents(String folderName) throws IllegalArgumentException, IOException {
        File uploadFolder = Paths.get(Configuration.getInstance().get("upload"), folderName).toFile();

        // Ensure the formed path is not a file
        if (uploadFolder.isFile()) {
            throw new IllegalArgumentException("Upload folder must not be a file.");
        }

        // @Security: Ensure that the result from combining the 'upload' folder and folder name is a child of
        // the 'upload folder'. Otherwise, the client could have access to files across the whole system.
        //
        // We should also consider if the path is a symbolic links that points outside of the directory. If so, that
        // could allow the client to get access to files outside of the 'upload' folder.
        if (!uploadFolder.getAbsolutePath().startsWith(config.get("upload")) || !uploadFolder.exists()) {
            throw new FileNotFoundException("no such folder exists.");
        }

        List<IEntry> entries = new ArrayList<>();
        File[] files = uploadFolder.listFiles();

        // Don't bother attempting loop through the directory entries if the
        // system folder was empty or couldn't be listed due to permissions.
        if (files == null) {
            return entries;
        }

        // Loop through each entry in the upload folder and convert them into FileEntry objects
        for (var file : files) {
            if (file.isFile()) {
                entries.add(new FileEntry(file.toPath()));
            } else {
                entries.add(new DirectoryEntry(file.toPath()));
            }
        }

        return entries;
    }
}
