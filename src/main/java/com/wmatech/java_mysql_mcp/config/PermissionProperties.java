package com.wmatech.java_mysql_mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.permissions")
public record PermissionProperties(
        boolean allowInsert,
        boolean allowUpdate,
        boolean allowDelete,
        boolean allowDdl
) {
}
