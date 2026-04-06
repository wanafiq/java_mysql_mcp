package com.wmatech.java_mysql_mcp.resource;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class MySqlTableResource {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public MySqlTableResource(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @McpResource(uri = "mysql://tables", name = "MySQL Tables",
            description = "Lists all tables in the database with metadata (row count, data size, index size, timestamps)")
    public String listTables() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList("""
                SELECT
                    TABLE_NAME AS tableName,
                    TABLE_ROWS AS estimatedRowCount,
                    DATA_LENGTH AS dataSizeBytes,
                    INDEX_LENGTH AS indexSizeBytes,
                    CREATE_TIME AS createTime,
                    UPDATE_TIME AS updateTime
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                ORDER BY TABLE_NAME
                """);
        return toJson(tables);
    }

    @McpResource(uri = "mysql://tables/{tableName}", name = "MySQL Table Schema",
            description = "Returns column details for a specific table (name, type, nullable, key, default, extra)")
    public String getTableSchema(String tableName) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT
                    COLUMN_NAME AS columnName,
                    DATA_TYPE AS dataType,
                    COLUMN_TYPE AS columnType,
                    IS_NULLABLE AS nullable,
                    COLUMN_KEY AS columnKey,
                    COLUMN_DEFAULT AS defaultValue,
                    EXTRA AS extra
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """, tableName);
        if (columns.isEmpty()) {
            return "Table not found: " + tableName;
        }
        return toJson(columns);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JacksonException e) {
            return "Error formatting result: " + e.getMessage();
        }
    }
}
