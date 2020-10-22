package server.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileEntry implements IEntry {
    private final Path path;
    private FileInputStream fileStream;
    private byte[] digest;
    private long size;


    public FileEntry(Path path) {
        this.path = path;
    }

    public void load() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            this.fileStream = new FileInputStream(String.valueOf(path));

            md.update(this.fileStream.readAllBytes());

            // Update our digest with the processed digest of the file stream.
            this.digest = md.digest();
            this.size = this.fileStream.getChannel().size();


        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] getDigest() {
        return this.digest;
    }

    public long getSize() {
        return this.size;
    }

    public FileInputStream getFileStream() {
        return this.fileStream;
    }

    @Override
    public EntryType getType() {
        return EntryType.FILE;
    }

    public Path getPath() {
        return this.path;
    }
}
