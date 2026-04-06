package com.wmatech.java_mysql_mcp.service;

import com.wmatech.java_mysql_mcp.sql.QueryType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public QueryService(JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    public QueryResult execute(String sql) {
        QueryType type = permissionService.validateAndGetType(sql);
        long start = System.currentTimeMillis();

        if (type == QueryType.SELECT) {
            return executeReadQuery(sql, start);
        } else {
            return executeWriteQuery(sql, start);
        }
    }

    @Transactional(readOnly = true)
    protected QueryResult executeReadQuery(String sql, long start) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        long elapsed = System.currentTimeMillis() - start;
        return new QueryResult(rows, rows.size(), elapsed);
    }

    @Transactional
    protected QueryResult executeWriteQuery(String sql, long start) {
        int affected = jdbcTemplate.update(sql);
        long elapsed = System.currentTimeMillis() - start;
        return new QueryResult(List.of(Map.of("affectedRows", affected)), affected, elapsed);
    }

    public record QueryResult(
            List<Map<String, Object>> rows,
            int rowCount,
            long executionTimeMs
    ) {
    }
}
