package common.resources;

import interfaces.IEntry;

import java.io.ByteArrayOutputStream;
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
     * The output stream of the resource when it needs to be converted into
     * a byte array.
     * */
    private ByteArrayOutputStream fileBuffer;

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
    public void load() {
        this.fileBuffer = new ByteArrayOutputStream();

        try (
                var fileStream = new FileInputStream(String.valueOf(path))
        ) {
            var md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int numRead;

            // Update our digest with the processed digest of the file stream.
            do {
                numRead = fileStream.read(buffer);
                if (numRead > 0) {
                    fileBuffer.write(buffer);
                }
            } while (numRead != -1);

            md.update(fileBuffer.toByteArray());

            this.digest = md.digest();
            this.size = fileBuffer.size();


        } catch (IOException | NoSuchAlgorithmException e) {
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
    public byte[] getFileBuffer() {
        return fileBuffer.toByteArray();
    }



    /**
     * Method that is used to return the 'file' type entry
     *
     * @return A {@link EntryType}, representing the file type.
     * */
    @Override
    public EntryType getType() {
        return EntryType.File;
    }

    /**
     * Method to get the path of the file.
     *
     * @return A {@link Path} that points to the entry resource.
     * */
    public Path getPath() {
        return this.path;
    }
}
