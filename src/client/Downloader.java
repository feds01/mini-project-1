package client;

import com.fasterxml.jackson.databind.JsonNode;
import common.protocol.Command;
import common.resources.FileEntry;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class Downloader extends BaseConnection implements Runnable {
    /**
     *
     */
    private final String filePath;

    /**
     *
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
     * The MD5 Signature of the file download, which will be used to
     * ensure that the file was successfully downloaded and it isn't
     * missing any content.
     */
    private final byte[] digest;

    /**
     *
     */
    private DownloaderStatus status = DownloaderStatus.NOT_STARTED;


    /**
     *
     */
    public Downloader(String host, int port, JsonNode info) {
        super(host, port);

        this.filePath = info.get("file").asText();
        this.size = info.get("size").asLong();
        this.digest = Base64.getDecoder().decode(info.get("digest").asText());
    }

    /**
     *
     */
    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    /**
     *
     */
    public void stop() {
        running.set(false);
        worker.interrupt();
    }

    /**
     *
     */
    @Override
    public void run() {
        super.run();

        this.running.set(true);
        this.status = DownloaderStatus.STARTED;

        Path filePath = getPathForResource();

        // hold a signature of the 'local' file version
        byte[] localDigest = new byte[]{};

        // Keep downloading the file until we can be sure that the 'local' version
        // has the same signature as the 'remote' version. Note that if the 'remote'
        // version of file changes, we have to account for this problem and hence
        // we should check for a digest mis-match every time.
        try {
            while (!Arrays.equals(this.digest, localDigest) && this.running.get()) {
                super.outputStream.printf("%s %s%n", Command.Download, this.filePath);

                // Download the file using the function
                var file = downloadFile(filePath.toString());

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

            e.printStackTrace();

            // We might have to delete any leftover resources that we're left by
            // writing an incomplete version of the file..
            boolean deleted = filePath.toFile().delete();

            if (!deleted) {
                System.err.println("Couldn't cleanup a failed download");
            }

            this.status = DownloaderStatus.FAILED;
        } finally {
            super.cleanup();
        }
    }

    /**
     *
     */
    private File downloadFile(String to) throws IOException {
        // @Consider: This is a dangerous operation since anyone could attempt
        //            to download a file that is larger than 2GB or 2^31 -1 bytes
        //            large. At that point it is probably safer to download the file
        //            in chunks.
        var fileBuffer = new byte[(int) this.size];

        var file = new File(to);

        // This is the 'try-with-resources' statement that will automatically close any resources
        // once the try block finishes executing. A good description of the syntax is present on
        // the webpage: https://www.javatpoint.com/java-try-with-resources
        try (
                var fileOutputStream = new FileOutputStream(file);
                var bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        ) {

            // Again, probably better to store these objects references in the support class
            DataInputStream dis = new DataInputStream(super.socket.getInputStream());

            int current = 0;

            while (current < (int) this.size) {
                fileBuffer[current++] = dis.readByte();

                this.progress = ((float) current / this.size) * 100f;
            }

            // finally, write it to the output stream...
            bufferedOutputStream.write(fileBuffer, 0, (int) this.size);
            bufferedOutputStream.flush();
        }

        return file;
    }


    /**
     *
     */
    private Path getPathForResource() {
        // create an output stream for the file in the 'downloads' folder.
        var originalFileOutputUri = Paths.get(BaseConnection.config.get("download"), Path.of(this.filePath).getFileName().toString());
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
     *
     */
    public String getProgressString() {
        String arrowIndicator = "=".repeat((int) (this.progress / 5)) + ">";

        return String.format("[%-21s] %.2f%% %4s with status %s", arrowIndicator, this.progress, this.filePath, this.status);
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
     *
     */
    @Override
    public void cleanup() {
        super.cleanup();

        // if the file didn't finish downloading, we need to remove it from the filesystem
        var file = Path.of(filePath).toFile();

        if (file.exists() && !this.status.equals(DownloaderStatus.FINISHED)) {
            // We might have to delete any leftover resources that we're left by
            // writing an incomplete version of the file..
            boolean deleted = Path.of(filePath).toFile().delete();

            if (!deleted) {
                System.err.println("Oops! Couldn't cleanup download.");
            }
        }
    }
}