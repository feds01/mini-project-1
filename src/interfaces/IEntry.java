package interfaces;

import common.resources.EntryType;

import java.nio.file.Path;

/**
 * Interface that represents either a Directory of File entry when the file
 * system lists the folders contents.
 *
 * @author 20008575
 * */
public interface IEntry {
    /**
     * Method that is used to return the type of entry, whether it's a file or
     * a directory.
     *
     * @return A {@link EntryType}, representing if it's a file or a directory.
     * */
    String getType();

    /**
     * Method to get the path of a file system entry.
     *
     * @return A {@link Path} that points to the entry resource.
     * */
    Path getPath();


    /**
     * Method used to return the top leaf of the entry's path.
     * */
    String getFileName();
}
