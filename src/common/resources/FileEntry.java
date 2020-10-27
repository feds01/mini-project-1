package common.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import interfaces.IEntry;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class that represents an filesystem entry of File type.
 *
 * @author 200008575
 * */
public class FileEntry implements IEntry {
    /**
     * Variable that represents the path that points to the resource.
     * */
    @JsonIgnore
    private final Path path;

    /**
     * The computed MD5 digest of the resource.
     * */
    private byte[] digest;

    /**
     * The size in bytes of the resource.
     * */
    private long size;

    /**
     * Class constructor.
     *
     * @param path - The path to the resource
     *
     * @throws IllegalArgumentException if the provided path doesn't exist or if it
     *                                  is not a file.
     * */
    public FileEntry(Path path) throws IllegalArgumentException {
        var file = path.toFile();

        if (!file.exists()) {
            throw new IllegalArgumentException("Path does not exist.");
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("Path cannot be a directory.");
        }

        this.path = path;
    }

    /**
     * Method that loads the resource. This method will also compute the size and digest of
     * the provided resource.
     * */
    public void load() throws IOException {
        try (
                var fileStream = new FileInputStream(String.valueOf(path))
        ) {
            var md = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[1024];
            int count;

            // Update our digest with the processed digest of the file stream.
            do {
                count = fileStream.read(buffer);
                if (count > 0) {

                    this.size += count;
                    md.update(buffer);
                }
            } while (count != -1);

            this.digest = md.digest();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to get the computed MD5 digest of the file.
     *
     * @return A byte array that represent the digest.
     * */
    public byte[] getDigest() {
        return this.digest;
    }

    /**
     * Method to get the computed size (in bytes) of the file.
     *
     * @return The size of the resource.
     * */
    public long getSize() {
        return this.size;
    }

    /**
     * Method to get the byte array representing the file.
     *
     * @return The byte array representing the file.
     * */
    @JsonIgnore
    public FileInputStream getInputStream() throws IOException {
        return new FileInputStream(String.valueOf(path));
    }



    /**
     * Method that is used to return the 'file' type entry
     *
     * @return A {@link EntryType}, representing the file type.
     * */
    @Override
    public String getType() {
        return EntryType.File.toString();
    }

    /**
     * Method to get the path of the file.
     *
     * @return A {@link Path} that points to the entry resource.
     * */
    @JsonIgnore
    public Path getPath() {
        return this.path;
    }

    /**
     * Utility method to fetch the top name of the file path
     *
     * @return The name of the leaf in the file path
     * */
    public String getFileName() {
        return this.path.getFileName().toString();
    }
}
