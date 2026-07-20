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
    /** SQL migrations already occupy versions 2 and 3; this records current logical schema V2. */
    private static final int CURRENT_SCHEMA_VERSION = 4;
    private static final Pattern VERSIONED_FILE = Pattern.compile("V(\\d+)__.+\\.sql");
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, applied_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS app_setting (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            applyBaseline(connection);
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/V*__*.sql");
            Arrays.sort(resources, Comparator.comparingInt(this::migrationVersion));
            for (Resource resource : resources) applyMigration(connection, resource);
            applyCurrentSchemaV2(connection);
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

    private boolean hasTable(Connection connection, String table) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (ResultSet tables = statement.executeQuery()) {
                return tables.next();
            }
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

    /**
     * Reconciles databases created by the short-lived {@code enabled}-based device UI
     * with the current {@code used}-based model. SQLite cannot drop a column in place,
     * so the table is rebuilt while retaining every registered device and its choice.
     * packet_log is the only confirmed unused legacy table: there is no current
     * entity, repository or reader for it, so it is explicitly removed.
     */
    private void applyCurrentSchemaV2(Connection connection) throws Exception {
        if (isApplied(connection, CURRENT_SCHEMA_VERSION)) return;
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            rebuildRegisteredDevicesIfNeeded(connection);
            dropLegacyTable(connection, "packet_log");
            recordVersion(connection, CURRENT_SCHEMA_VERSION);
            connection.commit();
            log.info("Applied current schema V2 cleanup (migration record V{})", CURRENT_SCHEMA_VERSION);
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private void rebuildRegisteredDevicesIfNeeded(Connection connection) throws Exception {
        if (!hasTable(connection, "registered_devices")) return;
        boolean hasEnabled = hasColumn(connection, "registered_devices", "enabled");
        boolean hasUsed = hasColumn(connection, "registered_devices", "used");
        if (!hasEnabled && hasUsed) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS registered_devices_v2");
            statement.execute("CREATE TABLE registered_devices_v2 ("
                    + "unique_id VARCHAR(255) NOT NULL PRIMARY KEY, "
                    + "object_id VARCHAR(255) NOT NULL, "
                    + "device_type VARCHAR(255) NOT NULL, "
                    + "index_number INTEGER NOT NULL, "
                    + "used BOOLEAN)");
            String usedExpression = hasUsed && hasEnabled ? "COALESCE(used, enabled)"
                    : hasUsed ? "used" : hasEnabled ? "enabled" : "NULL";
            statement.executeUpdate("INSERT INTO registered_devices_v2 (unique_id, object_id, device_type, index_number, used) "
                    + "SELECT unique_id, object_id, device_type, index_number, " + usedExpression
                    + " FROM registered_devices");
            statement.execute("DROP TABLE registered_devices");
            statement.execute("ALTER TABLE registered_devices_v2 RENAME TO registered_devices");
        }
        log.info("Rebuilt registered_devices for used-based publication (removed legacy enabled column)");
    }

    private void dropLegacyTable(Connection connection, String table) throws Exception {
        if (!hasTable(connection, table)) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE " + table);
        }
        log.info("Dropped unused legacy table: {}", table);
    }

    private boolean isDeviceTypeEmpty(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM device_type")) {
            return result.next() && result.getLong(1) == 0;
        }
    }

}
