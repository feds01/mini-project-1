package cli.printers;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Method used to print to the CLI a Table of resources which are available to be
 * downloaded from the peer with corresponding information about the resources.
 *
 * @author 200008575
 * */
public class ResourceTable {
    /**
     * The list of nodes that represent directory entries on the
     * */
    private final List<JsonNode> resourceList;

    /**
     * Method constructor to build a ResourceTable instance from a JsonNode
     * object that is returned from the 'list' command.
     *
     * @param info The response object received from the peer to be transformed
     *             into a printable table.
     * */
    public ResourceTable(JsonNode info) {
        var listNode = info.get("files");

        if (listNode == null) {
            throw new IllegalArgumentException("Response does not match expected format.");
        }

        this.resourceList = StreamSupport.stream(info.get("files").spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     * Method to construct and print the table of resources to the CLI
     * */
    public void print() {

        // Don't attempt to build a resource table if there are no resources
        if (this.resourceList.size() == 0) {
            System.out.println("No files");
            return;
        }

        var longestType = this.getLongestMember("type");
        var longestPath = this.getLongestMember("path");

        var tableFormat = "| %-" + longestType.length() + "s | %-" + longestPath.length() + "s |\n";

        System.out.printf(tableFormat, "Type", "Filename");

        // The Additional seven characters account for the spacing between the column header labels and barriers.
        var rowLength = 7 + longestPath.length() + longestType.length();

        System.out.println(this.getRow(rowLength));

        // now print the table entries
        resourceList.forEach(item -> System.out.printf(tableFormat, item.get("type").asText(), item.get("path").asText()));

        // finish off the table with a row at the bottom
        System.out.println(this.getRow(rowLength));

    }

    /**
     * Internal method to get a table separator row
     *
     * @return The constructed table row string
     * */
    private String getRow(int length) {
        return "-".repeat(length);
    }

    /**
     * Internal method to get the longest member (by character length) of a
     * field in the given resource list.
     *
     * @param fieldName The name of the field
     * @return The longest member of the field set.
     * */
    private String getLongestMember(String fieldName) {
        return Collections.max(resourceList.stream().map(item -> item.get(fieldName).asText())
                .collect(Collectors.toList()), Comparator.comparing(String::length));
    }
}
