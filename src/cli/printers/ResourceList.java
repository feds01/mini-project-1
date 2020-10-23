package cli.printers;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ResourceList {
    private final List<JsonNode> fileList;

    public ResourceList(JsonNode response) {
        var listNode = response.get("files");

        if (listNode == null) {
            throw new IllegalArgumentException("Response does not match expected format.");
        }

        this.fileList = StreamSupport.stream(response.get("files").spliterator(), false)
                .collect(Collectors.toList());
    }

    public void print() {
        var longestType = this.getLongestMember("type");
        var longestPath = this.getLongestMember("path");

        var tableFormat = "| %-" + longestType.length() + "s | %-" + longestPath.length() + "s |\n";

        System.out.printf(tableFormat, "Type", "Filename");

        // The Additional seven characters account for the spacing between the column header labels and barriers.
        var rowLength = 7 + longestPath.length() + longestType.length();

        System.out.println(this.getRow(rowLength));

        // now print the table entries
        fileList.forEach(item -> {
            System.out.printf(tableFormat, item.get("type").asText(), item.get("path").asText());
        });

        // finish off the table with a row at the bottom
        System.out.println(this.getRow(rowLength));

    }

    private String getRow(int length) {
        return "-".repeat(length);
    }

    private String getLongestMember(String fieldName) {
        return Collections.max(fileList.stream().map(item -> item.get(fieldName).asText())
                .collect(Collectors.toList()), Comparator.comparing(String::length));
    }
}
