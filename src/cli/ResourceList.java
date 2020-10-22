package cli;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ResourceList {
    public static void printResources(JsonNode response) {
        // Build a column map that will store the name of the column and the longest value of the column
        Map<String, Integer> columns = new LinkedHashMap<>();


        var fileList = StreamSupport.stream(response.get("files").spliterator(), false).collect(Collectors.toList());

        var longestType = Collections.max(fileList.stream().map(item -> item.get("type").asText()).collect(Collectors.toList()), Comparator.comparing(String::length));
        var longestPath = Collections.max(fileList.stream().map(item -> item.get("path").asText()).collect(Collectors.toList()), Comparator.comparing(String::length));

        var tableFormat = "| %-" + longestType.length() + "s | %-" + longestPath.length() + "s |\n";

        // print the table labels
        System.out.printf(tableFormat, "TYPE", "FIlENAME");

        // now print the table entries
        fileList.forEach(item -> {
            System.out.printf(tableFormat, item.get("type").asText(), item.get("path").asText());
        });
    }
}
