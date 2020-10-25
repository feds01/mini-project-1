package common.resources;

import interfaces.IEntry;

import java.nio.file.Path;

/**
 * Class that represents an filesystem entry of Directory type.
 *
 * @author 200008575
 * */
public class DirectoryEntry implements IEntry {
    /**
     * Variable that represents the path that points to the resource.
     * */
    private final Path path;

    /**
     * Class constructor.
     *
     * @param path - The path to the resource
     *
     * @throws IllegalArgumentException if the provided path doesn't exist or if it
     *                                  is not a directory
     * */
    public DirectoryEntry(Path path) {
        var file = path.toFile();

        if (!file.exists()) {
            throw new IllegalArgumentException("Path does not exist.");
        }

        if (!file.isDirectory()) {
            throw new IllegalArgumentException("Path cannot be a file.");
        }

        this.path = path;
    }

    /**
     * Method that is used to return the 'directory' type entry
     *
     * @return A {@link EntryType}, representing the directory type.
     * */
    @Override
    public EntryType getType() {
        return EntryType.Directory;
    }

    /**
     * Method to get the path of the directory.
     *
     * @return A {@link Path} that points to the entry resource.
     * */
    @Override
    public Path getPath() {
        return this.path;
    }
}
