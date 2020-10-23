package common.resources;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * */
public class FileEntry implements IEntry {
    /**
     *
     * */
    private final Path path;

    /**
     *
     * */
    private byte[] digest;

    /**
     *
     * */
    private long size;

    /**
     *
     * */
    private ByteArrayOutputStream fileBuffer;

    /**
     *
     * */
    public FileEntry(Path path) {
        this.path = path;
    }

    /**
     *
     * */
    public void load() {
        this.fileBuffer = new ByteArrayOutputStream();

        try (
                var fileStream = new FileInputStream(String.valueOf(path));
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
     *
     * */
    public byte[] getDigest() {
        return this.digest;
    }

    /**
     *
     * */
    public long getSize() {
        return this.size;
    }

    /**
     *
     * */
    public byte[] getFileBuffer() {
        return fileBuffer.toByteArray();
    }


    /**
     *
     * */
    @Override
    public EntryType getType() {
        return EntryType.FILE;
    }

    /**
     *
     * */
    public Path getPath() {
        return this.path;
    }
}
