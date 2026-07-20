package com.hyeonpyo.wallpadcontroller.settings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.hyeonpyo.wallpadcontroller.properties.Ew11Properties;
import com.hyeonpyo.wallpadcontroller.properties.Ew11TransportType;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private static final String CRYPTO_KEY = "system.encryption.key";
    private final JdbcTemplate jdbc;
    private final MqttProperties mqtt;
    private final Ew11Properties ew11;
    private final SettingsCrypto crypto;

    public synchronized ConnectionSettings initialize(boolean forceEnvironment) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS app_setting (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        String key = value(CRYPTO_KEY); if (key == null) { key = SettingsCrypto.newKey(); put(CRYPTO_KEY, key); } crypto.initialize(key);
        Map<String, String> defaults = environmentValues();
        defaults.forEach((name, setting) -> { if (forceEnvironment || value(name) == null) putImported(name, setting); });
        return current();
    }
    public synchronized ConnectionSettings current() { return from(values()); }
    public synchronized Map<String, Object> publicView() { ConnectionSettings s = current(); Map<String, Object> out = new LinkedHashMap<>(); out.put("mqttHost", s.mqttHost()); out.put("mqttPort", s.mqttPort()); out.put("mqttClientId", s.mqttClientId()); out.put("mqttUsername", s.mqttUsername()); out.put("mqttPasswordSet", !s.mqttPassword().isEmpty()); out.put("haTopic", s.haTopic()); out.put("transport", s.transport().name()); out.put("ew11MqttSendTopic", s.ew11MqttSendTopic()); out.put("ew11MqttReceiveTopic", s.ew11MqttReceiveTopic()); out.put("udpSendHost", s.udpSendHost()); out.put("udpSendPort", s.udpSendPort()); out.put("udpListenPort", s.udpListenPort()); out.put("udpBufferSize", s.udpBufferSize()); out.put("rebootEnabled", s.rebootEnabled()); out.put("rebootHost", s.rebootHost()); out.put("rebootUsername", s.rebootUsername()); out.put("rebootPasswordSet", !s.rebootPassword().isEmpty()); out.put("rebootInterval", s.rebootInterval()); return out; }
    public synchronized ConnectionSettings save(Map<String, String> input) {
        ConnectionSettings before = current();
        Map<String, String> next = values();
        input.forEach((k, v) -> { if (v != null) next.put(k, v.trim()); });
        if (next.getOrDefault("mqttPassword", "").isBlank()) next.put("mqttPassword", before.mqttPassword());
        if (next.getOrDefault("rebootPassword", "").isBlank()) next.put("rebootPassword", before.rebootPassword());
        ConnectionSettings settings = from(next); validate(settings);
        putAll(settings); return settings;
    }
    /** Keep legacy services on the currently persisted values until they are fully migrated. */
    public synchronized void applyProperties(ConnectionSettings s) {
        mqtt.setHost(s.mqttHost()); mqtt.setPort(s.mqttPort()); mqtt.setClientId(s.mqttClientId()); mqtt.setUsername(s.mqttUsername()); mqtt.setPassword(s.mqttPassword()); mqtt.setHaTopic(s.haTopic());
        ew11.setTransport(s.transport()); ew11.getMqtt().setSendTopic(s.ew11MqttSendTopic()); ew11.getMqtt().setReceiveTopic(s.ew11MqttReceiveTopic());
        ew11.getUdp().getSend().setHost(s.udpSendHost()); ew11.getUdp().getSend().setPort(s.udpSendPort()); ew11.getUdp().getListen().setPort(s.udpListenPort()); ew11.getUdp().getListen().setBufferSize(s.udpBufferSize());
        ew11.getReboot().setEnabled(s.rebootEnabled()); ew11.getReboot().setHost(s.rebootHost()); ew11.getReboot().setUsername(s.rebootUsername()); ew11.getReboot().setPassword(s.rebootPassword()); ew11.getReboot().setInterval(java.time.Duration.parse(s.rebootInterval()));
    }
    private Map<String, String> environmentValues() { Map<String,String> v = new LinkedHashMap<>(); v.put("mqttHost", mqtt.getHost()); v.put("mqttPort", String.valueOf(mqtt.getPort())); v.put("mqttClientId", mqtt.getClientId()); v.put("mqttUsername", Objects.toString(mqtt.getUsername(), "")); v.put("mqttPassword", Objects.toString(mqtt.getPassword(), "")); v.put("haTopic", mqtt.getHaTopic()); v.put("transport", ew11.getTransport().name()); v.put("ew11MqttSendTopic", ew11.getMqtt().getSendTopic()); v.put("ew11MqttReceiveTopic", ew11.getMqtt().getReceiveTopic()); v.put("udpSendHost", ew11.getUdp().getSend().getHost()); v.put("udpSendPort", String.valueOf(ew11.getUdp().getSend().getPort())); v.put("udpListenPort", String.valueOf(ew11.getUdp().getListen().getPort())); v.put("udpBufferSize", String.valueOf(ew11.getUdp().getListen().getBufferSize())); v.put("rebootEnabled", String.valueOf(ew11.getReboot().isEnabled())); v.put("rebootHost", Objects.toString(ew11.getReboot().getHost(), "")); v.put("rebootUsername", Objects.toString(ew11.getReboot().getUsername(), "")); v.put("rebootPassword", Objects.toString(ew11.getReboot().getPassword(), "")); v.put("rebootInterval", ew11.getReboot().getInterval().toString()); return v; }
    private Map<String,String> values() { Map<String,String> out = new LinkedHashMap<>(); jdbc.query("SELECT setting_key, setting_value FROM app_setting", rs -> { out.put(rs.getString(1), rs.getString(2)); }); return out; }
    private String value(String key) { var rows = jdbc.query("SELECT setting_value FROM app_setting WHERE setting_key=?", (rs, n) -> rs.getString(1), key); return rows.isEmpty() ? null : rows.getFirst(); }
    private void put(String key, String value) { jdbc.update("INSERT INTO app_setting(setting_key, setting_value, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value, updated_at=CURRENT_TIMESTAMP", key, value == null ? "" : value); }
    private void putImported(String key, String value) { put(key, isPassword(key) ? crypto.encrypt(value == null ? "" : value) : value); }
    private void putAll(ConnectionSettings s) { put("mqttHost", s.mqttHost()); put("mqttPort", "" + s.mqttPort()); put("mqttClientId", s.mqttClientId()); put("mqttUsername", s.mqttUsername()); put("mqttPassword", crypto.encrypt(s.mqttPassword())); put("haTopic", s.haTopic()); put("transport", s.transport().name()); put("ew11MqttSendTopic", s.ew11MqttSendTopic()); put("ew11MqttReceiveTopic", s.ew11MqttReceiveTopic()); put("udpSendHost", s.udpSendHost()); put("udpSendPort", "" + s.udpSendPort()); put("udpListenPort", "" + s.udpListenPort()); put("udpBufferSize", "" + s.udpBufferSize()); put("rebootEnabled", "" + s.rebootEnabled()); put("rebootHost", s.rebootHost()); put("rebootUsername", s.rebootUsername()); put("rebootPassword", crypto.encrypt(s.rebootPassword())); put("rebootInterval", s.rebootInterval()); }
    private ConnectionSettings from(Map<String,String> v) { return new ConnectionSettings(v.getOrDefault("mqttHost", ""), integer(v,"mqttPort"), v.getOrDefault("mqttClientId", "wallpad-controller"), v.getOrDefault("mqttUsername", ""), decode(v.get("mqttPassword")), v.getOrDefault("haTopic", "commax"), Ew11TransportType.valueOf(v.getOrDefault("transport", "MQTT").toUpperCase()), v.getOrDefault("ew11MqttSendTopic", "ew11/send"), v.getOrDefault("ew11MqttReceiveTopic", "ew11/recv"), v.getOrDefault("udpSendHost", "127.0.0.1"), integer(v,"udpSendPort"), integer(v,"udpListenPort"), integer(v,"udpBufferSize"), Boolean.parseBoolean(v.getOrDefault("rebootEnabled", "false")), v.getOrDefault("rebootHost", ""), v.getOrDefault("rebootUsername", ""), decode(v.get("rebootPassword")), v.getOrDefault("rebootInterval", "PT12H")); }
    private boolean isPassword(String key) { return "mqttPassword".equals(key) || "rebootPassword".equals(key); }
    private String decode(String value) { if (value == null || value.isEmpty()) return ""; try { return crypto.decrypt(value); } catch (RuntimeException ignored) { return value; } }
    private int integer(Map<String,String> v, String key) { try { return Integer.parseInt(v.getOrDefault(key, key.contains("Buffer") ? "512" : "0")); } catch (NumberFormatException e) { return 0; } }
    private void validate(ConnectionSettings s) { if (s.mqttHost().isBlank() || s.haTopic().isBlank() || s.mqttClientId().isBlank()) throw new IllegalArgumentException("MQTT host, client ID, HA topic은 필수입니다."); if (s.mqttPort()<1||s.mqttPort()>65535||s.udpSendPort()<1||s.udpSendPort()>65535||s.udpListenPort()<1||s.udpListenPort()>65535||s.udpBufferSize()<8||s.udpBufferSize()>65535) throw new IllegalArgumentException("포트 또는 UDP 버퍼 값이 올바르지 않습니다."); if (s.transport() == Ew11TransportType.MQTT && (s.ew11MqttSendTopic().isBlank() || s.ew11MqttReceiveTopic().isBlank())) throw new IllegalArgumentException("EW11 MQTT 송신/수신 topic은 필수입니다."); if (s.transport() == Ew11TransportType.UDP && s.udpSendHost().isBlank()) throw new IllegalArgumentException("UDP 송신 host는 필수입니다."); if (s.rebootEnabled() && s.rebootHost().isBlank()) throw new IllegalArgumentException("EW11 재부팅을 사용하려면 host가 필요합니다."); try { java.time.Duration.parse(s.rebootInterval()); } catch(Exception e) { throw new IllegalArgumentException("재부팅 주기는 ISO-8601 형식(예: PT12H)이어야 합니다."); } }
}
