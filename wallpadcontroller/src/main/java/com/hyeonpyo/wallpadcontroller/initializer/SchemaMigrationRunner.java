package com.hyeonpyo.wallpadcontroller.initializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Non-destructive SQLite schema changes for databases created before new features. */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class SchemaMigrationRunner implements CommandLineRunner {
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, applied_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS app_setting (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
            applyColumnIfMissing(connection, statement, "registered_devices", "enabled", "BOOLEAN NOT NULL DEFAULT 0");
            applyColumnIfMissing(connection, statement, "packet_log", "direction", "VARCHAR(8)");
            applyColumnIfMissing(connection, statement, "packet_log", "command_id", "VARCHAR(36)");
            recordVersion(connection, 1);
            log.info("✅ SQLite schema migration completed");
        }
    }

    private void applyColumnIfMissing(Connection connection, Statement statement, String table, String column, String definition) throws Exception {
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) return;
            }
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        log.info("🧩 SQLite column added: {}.{}", table, column);
    }

    private void recordVersion(Connection connection, int version) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO schema_version(version) VALUES (?)")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }
}
