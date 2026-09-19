package com.lauriel.store.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TestSchema {
    private TestSchema() {
    }

    public static String createIsolatedDatabase() throws Exception {
        String url = "jdbc:h2:mem:auth_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        String dump = Files.readString(Path.of("database/lauriel.sql"));
        Map<String, String> definitions = new LinkedHashMap<>();
        var tables = Pattern.compile("CREATE TABLE `(\\w+)` \\((.*?)\\) ENGINE=", Pattern.DOTALL).matcher(dump);
        while (tables.find()) {
            definitions.put(tables.group(1), "CREATE TABLE `" + tables.group(1) + "` (" + tables.group(2) + ")");
        }
        // Only CREATE TABLE bodies are read; dump DROP/GTID/data statements never execute.
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            Set<String> created = new HashSet<>();
            while (!definitions.isEmpty()) {
                boolean progressed = false;
                for (String name : new ArrayList<>(definitions.keySet())) {
                    String definition = definitions.get(name);
                    Set<String> references = new HashSet<>();
                    var foreignKeys = Pattern.compile("REFERENCES `(\\w+)`").matcher(definition);
                    while (foreignKeys.find()) {
                        references.add(foreignKeys.group(1));
                    }
                    if (created.containsAll(references)) {
                        statement.execute(definition);
                        created.add(name);
                        definitions.remove(name);
                        progressed = true;
                    }
                }
                if (!progressed) {
                    throw new IllegalStateException("Cannot order test schema foreign keys");
                }
            }
        }
        return url;
    }
}
