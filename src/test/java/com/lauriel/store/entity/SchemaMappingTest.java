package com.lauriel.store.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class SchemaMappingTest {
    @Test
    void mappingsMatchAllTablesColumnsKeysAndEnumsInOriginalDump() throws Exception {
        String sql = Files.readString(Path.of("database/lauriel.sql"));
        Map<String, String> tableDefinitions = new LinkedHashMap<>();
        var tables = Pattern.compile("CREATE TABLE `(\\w+)` \\((.*?)\\) ENGINE=", Pattern.DOTALL).matcher(sql);
        while (tables.find()) {
            tableDefinitions.put(tables.group(1), tables.group(2));
        }
        assertThat(tableDefinitions).hasSize(17);

        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        try {
            var sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var bean : scanner.findCandidateComponents("com.lauriel.store.entity")) {
                sources.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            }
            var metadata = sources.buildMetadata();
            // Boot the ORM model without a connection or any schema creation.
            try (var sessionFactory = metadata.buildSessionFactory()) {
                Set<String> mappedTables = new TreeSet<>();
                for (var entity : metadata.getEntityBindings()) {
                    var table = entity.getTable();
                    mappedTables.add(table.getName());
                    String definition = tableDefinitions.get(table.getName());
                    assertThat(definition).as(table.getName()).isNotNull();
                    Set<String> sqlColumns = new TreeSet<>();
                    var columns = Pattern.compile("(?m)^  `(\\w+)` ([^\\r\\n]+)").matcher(definition);
                    while (columns.find()) {
                        String columnName = columns.group(1);
                        String declaration = columns.group(2);
                        sqlColumns.add(columnName);
                        var column = table.getColumns().stream()
                                .filter(candidate -> candidate.getName().equals(columnName)).findFirst().orElseThrow();
                        assertThat(column.isNullable()).as(table.getName() + "." + columnName)
                                .isEqualTo(!declaration.contains("NOT NULL"));
                        var decimal = Pattern.compile("decimal\\((\\d+),(\\d+)\\)").matcher(declaration);
                        if (decimal.find()) {
                            assertThat(column.getPrecision()).isEqualTo(Integer.valueOf(decimal.group(1)));
                            assertThat(column.getScale()).isEqualTo(Integer.valueOf(decimal.group(2)));
                        }
                        var varchar = Pattern.compile("varchar\\((\\d+)\\)").matcher(declaration);
                        if (varchar.find()) {
                            assertThat(column.getLength()).isEqualTo(Long.valueOf(varchar.group(1)));
                        }
                        if (declaration.startsWith("enum(")) {
                            var enumField = Arrays.stream(entity.getMappedClass().getDeclaredFields())
                                    .filter(field -> field.isAnnotationPresent(Enumerated.class))
                                    .filter(field -> field.getAnnotation(jakarta.persistence.Column.class)
                                            .name().equals(columnName))
                                    .findFirst().orElseThrow();
                            Set<String> expectedValues = new TreeSet<>();
                            var values = Pattern.compile("'([^']+)'").matcher(
                                    declaration.substring(0, declaration.indexOf(')')));
                            while (values.find()) {
                                expectedValues.add(values.group(1));
                            }
                            assertThat(Arrays.stream(enumField.getType().getEnumConstants())
                                    .map(Object::toString).collect(Collectors.toSet()))
                                    .isEqualTo(expectedValues);
                        }
                    }
                    assertThat(table.getColumns().stream().map(column -> column.getName())
                            .collect(Collectors.toSet())).isEqualTo(sqlColumns);
                    var primaryKey = Pattern.compile("PRIMARY KEY \\(([^)]+)\\)").matcher(definition);
                    assertThat(primaryKey.find()).isTrue();
                    Set<String> keyColumns = Arrays.stream(primaryKey.group(1).replace("`", "").split(","))
                            .collect(Collectors.toSet());
                    assertThat(table.getPrimaryKey().getColumns().stream()
                            .map(column -> column.getName()).collect(Collectors.toSet())).isEqualTo(keyColumns);
                    Set<String> foreignKeys = new TreeSet<>();
                    var references = Pattern.compile("FOREIGN KEY \\(`(\\w+)`\\) REFERENCES `(\\w+)`")
                            .matcher(definition);
                    while (references.find()) {
                        foreignKeys.add(references.group(1) + "->" + references.group(2));
                    }
                    assertThat(table.getForeignKeys().values().stream()
                            .map(key -> key.getColumns().getFirst().getName() + "->" + key.getReferencedTable().getName())
                            .collect(Collectors.toSet())).isEqualTo(foreignKeys);
                }
                assertThat(mappedTables).isEqualTo(tableDefinitions.keySet());
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
