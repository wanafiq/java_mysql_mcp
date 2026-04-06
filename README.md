# java-mysql-mcp

A Java Spring Boot [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) server that gives AI assistants like Claude direct access to MySQL databases. It exposes SQL query execution and schema discovery as MCP tools and resources.

Built with Spring Boot 4, Spring AI, and STDIO transport -- designed so each project can configure its own database connection and permissions.

## Features

- **`mysql_query` tool** -- Execute SQL queries with configurable permission controls
- **`mysql://tables` resource** -- Browse all tables with metadata (row count, data size, index size, timestamps)
- **`mysql://tables/{tableName}` resource** -- Inspect column schema for any table
- **Permission system** -- Granular control over SELECT, INSERT, UPDATE, DELETE, and DDL operations
- **Per-project configuration** -- Each project passes its own database credentials via environment variables
- **SQL parsing** -- Robust query type detection using [JSqlParser](https://github.com/JSQLParser/JSqlParser) for security enforcement
- **Read-only by default** -- Only SELECT queries are allowed unless explicitly enabled

## Requirements

- Java 25+
- Maven 3.9+
- MySQL 8.0+

## Quick Start

### 1. Build

```bash
git clone https://github.com/wanafiq/java_mysql_mcp.git
cd java_mysql_mcp
./mvnw package -DskipTests
```

This produces `target/java-mysql-mcp-0.0.1-SNAPSHOT.jar`.

### 2. Configure your AI client

Add the MCP server to your client's configuration. The `env` block is where you specify which database to connect to and what operations are allowed.

#### Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "mysql": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/java-mysql-mcp-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "MYSQL_HOST": "127.0.0.1",
        "MYSQL_PORT": "3306",
        "MYSQL_USER": "root",
        "MYSQL_PASSWORD": "your_password",
        "MYSQL_DATABASE": "your_database",
        "ALLOW_INSERT": "false",
        "ALLOW_UPDATE": "false",
        "ALLOW_DELETE": "false",
        "ALLOW_DDL": "false"
      }
    }
  }
}
```

#### Claude Code

Add to your project's `.mcp.json` or `~/.claude.json`:

```json
{
  "mcpServers": {
    "mysql": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/java-mysql-mcp-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "MYSQL_HOST": "127.0.0.1",
        "MYSQL_PORT": "3306",
        "MYSQL_USER": "root",
        "MYSQL_PASSWORD": "your_password",
        "MYSQL_DATABASE": "your_database",
        "ALLOW_INSERT": "false",
        "ALLOW_UPDATE": "false",
        "ALLOW_DELETE": "false",
        "ALLOW_DDL": "false"
      }
    }
  }
}
```

### 3. Use it

Once connected, the AI assistant can:

- **Discover tables**: reads `mysql://tables` to see what's in the database
- **Inspect schema**: reads `mysql://tables/users` to see column definitions
- **Run queries**: calls `mysql_query` with SQL like `SELECT * FROM users LIMIT 10`

## Configuration

All configuration is done through environment variables, making it easy to use different databases per project.

### Connection

| Variable | Description | Default |
|---|---|---|
| `MYSQL_HOST` | MySQL server hostname | `localhost` |
| `MYSQL_PORT` | MySQL server port | `3306` |
| `MYSQL_USER` | MySQL username | `root` |
| `MYSQL_PASSWORD` | MySQL password | *(empty)* |
| `MYSQL_DATABASE` | Database name to connect to | *(empty)* |

### Permissions

All write operations are **disabled by default**. Enable them as needed:

| Variable | Description | Default |
|---|---|---|
| `ALLOW_INSERT` | Allow INSERT statements | `false` |
| `ALLOW_UPDATE` | Allow UPDATE statements | `false` |
| `ALLOW_DELETE` | Allow DELETE statements | `false` |
| `ALLOW_DDL` | Allow DDL statements (CREATE, ALTER, DROP, TRUNCATE) | `false` |

### Examples

**Read-only access** (safest -- default):

```json
"env": {
  "MYSQL_HOST": "127.0.0.1",
  "MYSQL_USER": "readonly_user",
  "MYSQL_PASSWORD": "secret",
  "MYSQL_DATABASE": "production_db"
}
```

**Full read/write access** (development):

```json
"env": {
  "MYSQL_HOST": "127.0.0.1",
  "MYSQL_USER": "dev_user",
  "MYSQL_PASSWORD": "secret",
  "MYSQL_DATABASE": "dev_db",
  "ALLOW_INSERT": "true",
  "ALLOW_UPDATE": "true",
  "ALLOW_DELETE": "true",
  "ALLOW_DDL": "true"
}
```

**Insert-only** (data ingestion):

```json
"env": {
  "MYSQL_HOST": "127.0.0.1",
  "MYSQL_USER": "ingest_user",
  "MYSQL_PASSWORD": "secret",
  "MYSQL_DATABASE": "analytics_db",
  "ALLOW_INSERT": "true"
}
```

## MCP Tools

### mysql_query

Execute a SQL query against the connected MySQL database.

**Parameters:**

| Name | Type | Required | Description |
|---|---|---|---|
| `sql` | string | yes | The SQL query to execute |

**Response format (SELECT):**

```json
{
  "rows": [
    { "id": 1, "name": "Alice", "email": "alice@example.com" },
    { "id": 2, "name": "Bob", "email": "bob@example.com" }
  ],
  "rowCount": 2,
  "executionTimeMs": 12
}
```

**Response format (INSERT/UPDATE/DELETE):**

```json
{
  "rows": [
    { "affectedRows": 3 }
  ],
  "rowCount": 3,
  "executionTimeMs": 45
}
```

**Permission denied response:**

```
Permission denied: INSERT operations are not allowed
Allowed operations: SELECT
```

## MCP Resources

### mysql://tables

Lists all tables in the connected database with metadata.

**Response:**

```json
[
  {
    "tableName": "users",
    "estimatedRowCount": 15228,
    "dataSizeBytes": 2637824,
    "indexSizeBytes": 2129920,
    "createTime": "2026-03-31T15:00:47",
    "updateTime": null
  }
]
```

### mysql://tables/{tableName}

Returns column details for a specific table.

**Example:** `mysql://tables/users`

**Response:**

```json
[
  {
    "columnName": "id",
    "dataType": "bigint",
    "columnType": "bigint unsigned",
    "nullable": "NO",
    "columnKey": "PRI",
    "defaultValue": null,
    "extra": "auto_increment"
  },
  {
    "columnName": "name",
    "dataType": "varchar",
    "columnType": "varchar(255)",
    "nullable": "YES",
    "columnKey": "",
    "defaultValue": null,
    "extra": ""
  }
]
```

## Testing with MCP Inspector

[MCP Inspector](https://github.com/modelcontextprotocol/inspector) (v0.21.1+) is a visual testing and debugging tool for MCP servers. It provides a web UI to connect to your server, browse its capabilities, and invoke tools/resources interactively.

### Install and run

```bash
# Run directly (no install needed, downloads to npx cache)
npx @modelcontextprotocol/inspector@0.21.1

# Or install globally
npm install -g @modelcontextprotocol/inspector@0.21.1
mcp-inspector
```

This starts two services:
- **Inspector UI** at `http://localhost:6274` -- the web interface you interact with
- **MCP Proxy** at `http://localhost:6277` -- bridges the UI to your STDIO server

### Connect to java-mysql-mcp

In the Inspector UI at `http://localhost:6274`:

1. Set **Transport** to `STDIO`
2. Set **Command** to `java`
3. Set **Arguments** to `-jar /absolute/path/to/java-mysql-mcp-0.0.1-SNAPSHOT.jar`
4. Add your environment variables under **Environment Variables**:

   | Key | Value |
   |---|---|
   | `MYSQL_HOST` | `127.0.0.1` |
   | `MYSQL_PORT` | `3306` |
   | `MYSQL_USER` | `root` |
   | `MYSQL_PASSWORD` | *your password* |
   | `MYSQL_DATABASE` | *your database* |

5. Click **Connect**

### Inspector tabs

Once connected, the Inspector provides three tabs matching MCP capabilities:

| Tab | What it shows | Try this |
|---|---|---|
| **Tools** | Lists all tools with their JSON schema, descriptions, and parameter definitions | Select `mysql_query`, enter `SELECT * FROM your_table LIMIT 5` in the `sql` field, click Run |
| **Resources** | Lists all resources with their URIs | Click `mysql://tables` to see all tables, or enter a table name in `mysql://tables/{tableName}` |
| **Prompts** | Lists prompt templates (none currently exposed) | -- |

### Custom ports

If the default ports conflict with other services:

```bash
# Custom UI port (CLIENT_PORT) and proxy port (SERVER_PORT)
CLIENT_PORT=8080 SERVER_PORT=8081 npx @modelcontextprotocol/inspector@0.21.1
```

## Architecture

```
com.wmatech.java_mysql_mcp
├── JavaMysqlMcpApplication.java        # Spring Boot entry point
├── config/
│   └── PermissionProperties.java       # @ConfigurationProperties for permissions
├── tool/
│   └── MySqlQueryTool.java             # @McpTool: mysql_query
├── resource/
│   └── MySqlTableResource.java         # @McpResource: table listing & schema
├── service/
│   ├── QueryService.java               # SQL execution with read/write transactions
│   ├── PermissionService.java          # SQL type validation against permissions
│   └── PermissionDeniedException.java  # Thrown when operation is not allowed
└── sql/
    ├── SqlParser.java                  # JSqlParser-based query type detection
    └── QueryType.java                  # Enum: SELECT, INSERT, UPDATE, DELETE, DDL, OTHER
```

### How it works

1. AI client sends a `mysql_query` tool call with a SQL string
2. **SqlParser** parses the SQL using JSqlParser to determine the query type
3. **PermissionService** checks if that query type is allowed
4. **QueryService** executes the query:
   - SELECT: runs in a read-only transaction, returns result rows
   - INSERT/UPDATE/DELETE/DDL: runs in a transaction with commit/rollback, returns affected row count
5. Results are returned as JSON to the AI client

## Security Considerations

- **Read-only by default**: All write operations must be explicitly enabled
- **SQL parsing**: Queries are parsed by JSqlParser before execution to accurately detect the operation type -- comments, CTEs, and other tricks cannot bypass permission checks
- **Transaction safety**: SELECT runs in read-only transactions; write operations use transactions with automatic rollback on failure
- **No raw credential storage**: Database credentials are passed as environment variables, never stored in config files
- **Connection pooling**: HikariCP manages connections with a configurable pool size (default: 10)

**Recommendations:**

- Use a dedicated MySQL user with minimal privileges for each project
- Keep write permissions disabled in production environments
- Never expose the MCP server over HTTP without authentication (STDIO is local-only by design)

## Tech Stack

| Component | Version |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.5 |
| Spring AI MCP | 2.0.0-M4 |
| JSqlParser | 5.3 |
| MySQL Connector/J | *(managed by Spring Boot)* |
| HikariCP | *(managed by Spring Boot)* |
| JUnit | 6 |
| Testcontainers | 1.21.4 |

## Development

### Build

```bash
./mvnw clean package
```

### Run tests

```bash
./mvnw test
```

### Run locally

```bash
MYSQL_HOST=localhost MYSQL_DATABASE=mydb MYSQL_USER=root MYSQL_PASSWORD=secret \
  java -jar target/java-mysql-mcp-0.0.1-SNAPSHOT.jar
```

## Roadmap

- [ ] HTTP/SSE transport for remote deployment
- [ ] Schema-specific permission overrides
- [ ] Multi-database mode
- [ ] Query result pagination
- [ ] Unix socket connections

## License

MIT
