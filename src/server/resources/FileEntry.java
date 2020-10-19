package server.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileEntry implements IEntry {
    private final Path path;
    private InputStream fileStream;
    private byte[] digest;


    public FileEntry(Path path) {
        this.path = path;
    }

    public void load() {
        try  {
            MessageDigest md = MessageDigest.getInstance("MD5");

            this.fileStream = Files.newInputStream(path);

            md.update(this.fileStream.readAllBytes());

            // Update our digest with the processed digest of the file stream.
            this.digest = md.digest();


        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] getDigest() {
        return this.digest;
    }

    public InputStream getFileStream() {
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
