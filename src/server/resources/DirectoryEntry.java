package server.resources;

import java.nio.file.Path;

public class DirectoryEntry implements IEntry {
    private final Path path;


    public DirectoryEntry(Path path) {
        this.path = path;
    }

    @Override
    public EntryType getType() {
        return EntryType.FILE;
    }

    @Override
    public Path getPath() {
        return this.path;
    }
}
