package com.hyeonpyo.wallpadcontroller.initializer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Applies ordered, non-destructive SQLite migrations from db/migration. */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SchemaMigrationRunner implements CommandLineRunner {
    private static final int BASELINE_VERSION = 1;
    private static final Pattern VERSIONED_FILE = Pattern.compile("V(\\d+)__.+\\.sql");
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, applied_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS app_setting (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            applyColumnIfMissing(statement, "registered_devices", "enabled", "BOOLEAN NOT NULL DEFAULT 0");
            applyColumnIfMissing(statement, "packet_log", "direction", "VARCHAR(8)");
            applyColumnIfMissing(statement, "packet_log", "command_id", "VARCHAR(36)");

            applyBaseline(connection);
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/V*__*.sql");
            Arrays.sort(resources, Comparator.comparingInt(this::migrationVersion));
            for (Resource resource : resources) applyMigration(connection, resource);
            log.info("SQLite schema migrations completed");
        }
    }

    /**
     * Version 1 owns the initial Commax definitions.  A database with existing
     * definitions but no version history is treated as a legacy V1 database;
     * this prevents re-seeding or deleting user data during an upgrade.
     */
    private void applyBaseline(Connection connection) throws Exception {
        if (isApplied(connection, BASELINE_VERSION)) return;

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if (isDeviceTypeEmpty(connection)) {
                Resource initialSql = new PathMatchingResourcePatternResolver()
                        .getResource("classpath:commax-initial.sql");
                if (!initialSql.exists()) {
                    throw new FileNotFoundException("Initial Commax schema was not found: commax-initial.sql");
                }
                executeScript(connection, initialSql);
                log.info("Applied baseline V{}: Commax initial definitions", BASELINE_VERSION);
            } else {
                log.info("Recorded baseline V{} for existing Commax database", BASELINE_VERSION);
            }
            recordVersion(connection, BASELINE_VERSION);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private int migrationVersion(Resource resource) {
        Matcher matcher = VERSIONED_FILE.matcher(resource.getFilename());
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }

    private void applyMigration(Connection connection, Resource resource) throws Exception {
        Matcher matcher = VERSIONED_FILE.matcher(resource.getFilename());
        if (!matcher.matches()) return;
        int version = Integer.parseInt(matcher.group(1));
        if (isApplied(connection, version)) return;

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            executeScript(connection, resource);
            recordVersion(connection, version);
            connection.commit();
            log.info("Applied migration V{}: {}", version, resource.getFilename());
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void executeScript(Connection connection, Resource resource) throws Exception {
        try (InputStream input = resource.getInputStream(); Statement statement = connection.createStatement()) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$", "");
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) executeMigrationStatement(connection, statement, trimmed);
            }
        }
    }

    private void executeMigrationStatement(Connection connection, Statement statement, String sql) throws Exception {
        Matcher alter = Pattern.compile("ALTER TABLE (\\w+) ADD COLUMN (\\w+).*", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (alter.matches() && hasColumn(connection, alter.group(1), alter.group(2))) return;
        statement.execute(sql);
    }

    private boolean hasColumn(Connection connection, String table, String column) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) if (column.equalsIgnoreCase(columns.getString("name"))) return true;
            return false;
        }
    }

    private boolean isApplied(Connection connection, int version) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM schema_version WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void recordVersion(Connection connection, int version) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO schema_version(version) VALUES (?)")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }

    private boolean isDeviceTypeEmpty(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM device_type")) {
            return result.next() && result.getLong(1) == 0;
        }
    }

    private void applyColumnIfMissing(Statement statement, String table, String column, String definition) throws Exception {
        if (!hasTable(statement, table)) return;
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) if (column.equalsIgnoreCase(columns.getString("name"))) return;
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private boolean hasTable(Statement statement, String table) throws Exception {
        try (ResultSet tables = statement.executeQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '" + table + "'")) {
            return tables.next();
        }
    }
}
