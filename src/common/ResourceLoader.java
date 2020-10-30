package common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility method to interact with application resources.
 *
 * @author 200008575
 * */
public class ResourceLoader {

    /**
     * Method to create an InputStream from a resource specified by a
     * pathname which is either in the executable or in the local build files.
     *
     * @param resourceName - The path of the resource to be loaded.
     *
     * @return An inputStream that points to the specified resource.
     * */
    public static InputStream load(String resourceName) throws IOException {

        var stream =  ResourceLoader.class
                .getClassLoader()
                .getResourceAsStream(resourceName);

        // Couldn't load the resource for some reason.
        if (stream == null) {
            throw new IOException("Couldn't load resource.");
        }

        return stream;
    }
}
