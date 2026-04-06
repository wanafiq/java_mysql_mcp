package com.wmatech.java_mysql_mcp.tool;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.wmatech.java_mysql_mcp.service.PermissionDeniedException;
import com.wmatech.java_mysql_mcp.service.PermissionService;
import com.wmatech.java_mysql_mcp.service.QueryService;
import com.wmatech.java_mysql_mcp.service.QueryService.QueryResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MySqlQueryTool {

    private final QueryService queryService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    public MySqlQueryTool(QueryService queryService, PermissionService permissionService, ObjectMapper objectMapper) {
        this.queryService = queryService;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @McpTool(name = "mysql_query", description = "Execute a SQL query against the MySQL database. SELECT is always allowed. Other operations depend on server configuration.")
    public String query(
            @McpToolParam(description = "The SQL query to execute", required = true) String sql) {
        try {
            QueryResult result = queryService.execute(sql);
            return formatResult(result);
        } catch (PermissionDeniedException e) {
            return "Permission denied: " + e.getMessage() + "\n" + permissionService.describePermissions();
        } catch (Exception e) {
            return "Error executing query: " + e.getMessage();
        }
    }

    private String formatResult(QueryResult result) {
        try {
            Map<String, Object> response = Map.of(
                    "rows", result.rows(),
                    "rowCount", result.rowCount(),
                    "executionTimeMs", result.executionTimeMs()
            );
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JacksonException e) {
            return "Error formatting result: " + e.getMessage();
        }
    }
}
