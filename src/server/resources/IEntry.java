package server.resources;

import java.nio.file.Path;

public interface IEntry {
    public EntryType getType();
    public Path getPath();
}
