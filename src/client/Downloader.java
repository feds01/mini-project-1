package client;

import com.fasterxml.jackson.databind.JsonNode;
import common.BaseConnection;
import common.protocol.Command;
import common.resources.FileEntry;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloader class used to start up an isolated connection that will
 * independently download a resource from the peer.
 *
 * @author 200008575
 */
public class Downloader extends BaseConnection implements Runnable {
    /**
     * Name of the file
     */
    private final String fileName;

    /**
     * The path to the location where the resource will be saved
     * */
    private final Path downloadLocation;

    /**
     * The thread instance that is used to run the downloader instance on.
     */
    private Thread worker;

    /**
     * Variable to hold the running status of the server. This variable must be atomic
     * since the server instance can be accessed within multiple threads.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);


    /**
     * The size of the file download in bytes
     */
    private final long size;

    /**
     * The percentage progress of the file download.
     */
    private float progress = 0;

    /**
     * The MD5 digest of the file download, which will be used to
     * ensure that the file was successfully downloaded and it isn't
     * missing any content.
     */
    private final byte[] digest;

    /**
     * Variable representing the status of the download
     */
    private DownloaderStatus status = DownloaderStatus.NOT_STARTED;


    /**
     * Method constructor to instantiate a Downloader object.
     *
     * @param host - The hostname of the peer
     * @param port - The port on the hostname of the peer
     * @param info - Information received from the peer about the downloaded
     *             resource that will be used to download the current file.
     */
    public Downloader(String host, int port, Path downloadLocation, JsonNode info) throws IOException {
        super(host, port);

        // get the important metadata from the info object
        this.downloadLocation = downloadLocation;
        this.fileName = info.get("fileName").asText();
        this.size = info.get("size").asLong();
        this.digest = Base64.getDecoder().decode(info.get("digest").asText());
    }


    /**
     * Method to start the downloader.
     */
    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    /**
     * Method to stop the downloader.
     */
    public void stop() {
        running.set(false);
        worker.interrupt();
    }

    /**
     * Method that will begin the downloading of the resource. The method
     * will first acquire a local path that will be used to represent the
     * downloaded resource. The method will then send a {@link Command} 'Download'
     * request to the server which will make the server begin writing to a
     * {@link DataOutputStream} that will be read. The file will be downloaded
     * and written to the given path. After finishing downloading the file, an
     * MD5 digest of the local file will be computed to check that the downloaded
     * resource is the same as the remote resource. Due to networking connections, some
     * packets may of dropped or the resource could of changed during the time of
     * downloading the file. If the MD5 digests are not equivalent, the downloader
     * will re-attempt to download the resource until the digests match.
     */
    @Override
    public void run() {
        this.running.set(true);
        this.status = DownloaderStatus.STARTED;

        // hold a signature of the 'local' file version
        byte[] localDigest = new byte[]{};

        // Keep downloading the file until we can be sure that the 'local' version
        // has the same signature as the 'remote' version. Note that if the 'remote'
        // version of file changes, we have to account for this problem and hence
        // we should check for a digest mis-match every time.
        try {
            while (!Arrays.equals(this.digest, localDigest) && this.running.get()) {
                // Send a request to the server to send the file as a byte array stream
                this.printWriter.printf("%s %s%n", Command.Get, this.fileName);

                // Download the file using the function
                var file = downloadFile(downloadLocation.toString());

                var fileEntry = new FileEntry(Path.of(file.getAbsolutePath()));
                fileEntry.load();

                localDigest = fileEntry.getDigest();

                if (!Arrays.equals(this.digest, localDigest)) {
                    this.status = DownloaderStatus.MISMATCHING_SIGNATURE;
                }
            }

            // Set the download status as completed and invoke the supper defined function to clean up
            // resources that are inherited from the connection base class.
            this.status = DownloaderStatus.FINISHED;
        } catch (IOException e) {
            this.status = DownloaderStatus.FAILED;
        }  finally {
            this.cleanup();
        }
    }

    /**
     * Method to read the file from the socket connection. The method
     * will write the received byte array to the path that is given
     * to the method.
     *
     * @param to - The path of the file that the byte array will be written to.
     *
     * @return A {@link File} representing where the file was downloaded to.
     */
    private File downloadFile(String to) throws IOException {
        // Use a small temporary buffer to hold the data which will be
        // immediately written to the file.
        var fileBuffer = new byte[4096];

        var file = new File(to);

        // This is the 'try-with-resources' statement that will automatically close any resources
        // once the try block finishes executing. A good description of the syntax is present on
        // the webpage: https://www.javatpoint.com/java-try-with-resources
        try (
                var fileOutputStream = new FileOutputStream(file);
                var bufferedOutputStream = new BufferedOutputStream(fileOutputStream)
        ) {

            // Again, probably better to store these objects references in the support class
            DataInputStream dis = new DataInputStream(this.socket.getInputStream());

            int count;
            int total = 0;

            while ((count = dis.read(fileBuffer)) > 0) {
                bufferedOutputStream.write(fileBuffer, 0, count);

                total += count;
                this.progress = ((float) total / this.size) * 100f;
            }

            // @Workaround: What if the file that was being downloaded has a size of zero?
            // We'll check it here, and if this is the case we will just set the progress
            // to 100.0% since we still need to create the file on the local file system.
            if (this.size == 0) {
                this.progress = 100f;
            }

            // finally, write it to the output stream...
            bufferedOutputStream.flush();
        }

        return file;
    }


    /**
     * Method used to acquire a path on the local computer in the 'download' folder.
     * If the current filePath is already taken by another resource on the local system,
     * the method construct a path with a suffix such as 'file (1).extension' and will
     * check if it exists too. The method will continue incrementing the suffix until
     * it finds an un-taken file path.
     *
     * Some examples of the process:
     * file.txt     -> file (1).txt
     * file (2).txt -> file (3).txt
     * file         -> file (1)
     *
     * @return A {@link Path} that will be used to write the downloaded resource to.
     */
    public static Path getPathForResource(String filename) {
        // create an output stream for the file in the 'downloads' folder.
        var originalFileOutputUri = Paths.get(BaseConnection.config.get("download"), filename);
        var fileOutputURI = originalFileOutputUri;

        // check if the file already exists on our side, otherwise attempt to add a suffix
        // thus avoiding cancelling the operation.
        var count = 1;

        while (fileOutputURI.toFile().exists()) {
            var sepIndex = originalFileOutputUri.toString().lastIndexOf('.');

            // file doesn't have an extension
            if (sepIndex == -1) {
                fileOutputURI = Path.of(String.format("%s (%s)", originalFileOutputUri, count));
            } else {
                var basename = originalFileOutputUri.toString().substring(0, sepIndex);
                var extension = originalFileOutputUri.toString().substring(sepIndex + 1);

                fileOutputURI = Path.of(String.format("%s (%s).%s", basename, count, extension));
            }
            count++;
        }

        return fileOutputURI;
    }

    /**
     * Method to get the download progress string of the download file.
     *
     * @return A string that's formed from the status and progress of the download.
     */
    public String getProgressString() {
        String arrowIndicator = "=".repeat((int) (this.progress / 5)) + ">";

        return String.format("[%-21s] %.2f%% %4s with status %s", arrowIndicator, this.progress, this.fileName, this.status);
    }


    /**
     * Method used to access the current state of the download thread.
     *
     * @return The current status of the download.
     */
    public DownloaderStatus getStatus() {
        return status;
    }

    /**
     * Method to clean up any resources that the downloader still has open. The method will
     * also delete the file that it was writing the resource to if the status of the
     * downloader hasn't finished. It is deleted because that means that the resource didn't
     * finish downloading when the downloading unexpectedly stopped.
     */
    @Override
    public void cleanup() {
        super.cleanup();

        // if the file didn't finish downloading, we need to remove it from the filesystem
        var file = downloadLocation.toFile();

        if (file.exists() && !this.status.equals(DownloaderStatus.FINISHED)) {
            // We might have to delete any leftover resources that we're left by
            // writing an incomplete version of the file..
            boolean deleted = downloadLocation.toFile().delete();

            if (!deleted) {
                System.err.println("Oops! Couldn't cleanup download.");
            }
        }
    }
}