package com.hyeonpyo.wallpadcontroller.initializer;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

class SchemaMigrationRunnerTest {
    @Test
    void rebuildsEnabledBasedDeviceTableAndDropsOnlyKnownLegacyPacketLog() throws Exception {
        String url = "jdbc:sqlite:file:schema-v2-" + UUID.randomUUID() + "?mode=memory&cache=shared";
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE schema_version (version INTEGER PRIMARY KEY, applied_at DATETIME)");
            statement.execute("INSERT INTO schema_version(version) VALUES (1), (2), (3)");
            statement.execute("CREATE TABLE registered_devices (unique_id TEXT PRIMARY KEY, object_id TEXT NOT NULL, device_type TEXT NOT NULL, index_number INTEGER NOT NULL, enabled BOOLEAN NOT NULL, used BOOLEAN)");
            statement.execute("INSERT INTO registered_devices VALUES ('commax_EV_1', 'old', 'EV', 1, 1, NULL)");
            statement.execute("CREATE TABLE packet_log (id INTEGER PRIMARY KEY, raw_data TEXT)");
            statement.execute("CREATE TABLE user_kept_table (id INTEGER PRIMARY KEY)");

            var dataSource = new SimpleDriverDataSource();
            dataSource.setDriverClass(org.sqlite.JDBC.class); dataSource.setUrl(url);
            new SchemaMigrationRunner(dataSource).run();

            try (var columns = statement.executeQuery("PRAGMA table_info(registered_devices)")) {
                boolean enabledFound = false; boolean usedFound = false;
                while (columns.next()) { enabledFound |= "enabled".equals(columns.getString("name")); usedFound |= "used".equals(columns.getString("name")); }
                assertThat(enabledFound).isFalse(); assertThat(usedFound).isTrue();
            }
            try (var result = statement.executeQuery("SELECT object_id, used FROM registered_devices WHERE unique_id = 'commax_EV_1'")) {
                assertThat(result.next()).isTrue(); assertThat(result.getString(1)).isEqualTo("old"); assertThat(result.getBoolean(2)).isTrue();
            }
            assertThat(exists(statement, "packet_log")).isFalse();
            assertThat(exists(statement, "user_kept_table")).isTrue();
            assertThat(exists(statement, "schema_version")).isTrue();
        }
    }

    private static boolean exists(java.sql.Statement statement, String table) throws Exception {
        try (var result = statement.executeQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + table + "'")) { return result.next(); }
    }
}
