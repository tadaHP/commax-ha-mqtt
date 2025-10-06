package com.hyeonpyo.wallpadcontroller.mqtt.discovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import static com.hyeonpyo.wallpadcontroller.domain.device.DeviceKey.*;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttDiscoveryPublisher {

    private final DeviceEntityRepository deviceEntityRepository;
    private final MqttClient mqttClient;

    private final MqttProperties mqttProperties;
    private static final String DISCOVERY_PREFIX = "homeassistant";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void publishDiscovery() {
        List<DeviceEntity> devices = deviceEntityRepository.findAll();

        for (DeviceEntity device : devices) {
            try {
                String baseId = device.getType().name() + device.getIndex();
                String objectId = device.getObjectId();
                String uniqueId = device.getUniqueId();

                Map<String, Object> payload = new HashMap<>();
                payload.put("name", baseId);
                payload.put("unique_id", uniqueId);
                payload.put("device", Map.of(
                        "identifiers", List.of("commax_wallpad"),
                        "name", "commax_wallpad",
                        "model", "commax_wallpad",
                        "manufacturer", "commax_wallpad"
                ));
                payload.put("availability", List.of(Map.of(
                        "topic", mqttProperties.getHaTopic() + "/status",
                        "payload_available", "online",
                        "payload_not_available", "offline"
                )));

                String topic = null;
                switch (device.getType()) {
                    case Light -> {
                        topic = DISCOVERY_PREFIX + "/light/" + baseId + "/config";
                        payload.put("default_entity_id", "light." + objectId);
                        payload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/power");
                        payload.put("command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/power");
                        payload.put("payload_on", "ON");
                        payload.put("payload_off", "OFF");
                    }
                    case LightBreaker, Outlet -> {
                        topic = DISCOVERY_PREFIX + "/switch/" + baseId + "/config";
                        payload.put("default_entity_id", "switch." + objectId);
                        payload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/power");
                        payload.put("command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/power");
                        payload.put("payload_on", "ON");
                        payload.put("payload_off", "OFF");
                    
                        // 콘센트 전용 기능 추가
                        if (device.getType() == Outlet) {
                            // ecomode switch
                            String ecoTopic = DISCOVERY_PREFIX + "/switch/" + baseId + "_ecomode/config";
                            Map<String, Object> ecoPayload = new HashMap<>(payload);
                            ecoPayload.put("name", baseId + " 자동대기전력차단");
                            ecoPayload.put("default_entity_id", "switch." + objectId + "_ecomode");
                            ecoPayload.put("unique_id", uniqueId + "_ecomode");
                            ecoPayload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/ecomode");
                            ecoPayload.put("command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/ecomode");
                            mqttClient.publish(ecoTopic, objectMapper.writeValueAsBytes(ecoPayload), 1, true);
                        
                            // cutoff value number
                            String cutoffTopic = DISCOVERY_PREFIX + "/number/" + baseId + "_cutoff_value/config";
                            Map<String, Object> cutoffPayload = new HashMap<>(payload);
                            cutoffPayload.put("name", baseId + " 자동대기전력차단값");
                            cutoffPayload.put("default_entity_id", "number." + objectId + "_cutoff_value");
                            cutoffPayload.put("unique_id", uniqueId + "_cutoff_value");
                            cutoffPayload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/cutoff");
                            cutoffPayload.put("command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/setCutoff");
                            cutoffPayload.put("step", 1);
                            cutoffPayload.put("min", 0);
                            cutoffPayload.put("max", 500);
                            cutoffPayload.put("mode", "box");
                            cutoffPayload.put("unit_of_measurement", "W");
                            mqttClient.publish(cutoffTopic, objectMapper.writeValueAsBytes(cutoffPayload), 1, true);
                        
                            // power sensor
                            String wattTopic = DISCOVERY_PREFIX + "/sensor/" + baseId + "_watt/config";
                            Map<String, Object> wattPayload = new HashMap<>(payload);
                            wattPayload.put("name", baseId + " 소비전력");
                            wattPayload.put("default_entity_id", "sensor." + objectId + "_watt");
                            wattPayload.put("unique_id", uniqueId + "_watt");
                            wattPayload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/watt");
                            wattPayload.put("unit_of_measurement", "W");
                            wattPayload.put("device_class", "power");
                            wattPayload.put("state_class", "measurement");
                            mqttClient.publish(wattTopic, objectMapper.writeValueAsBytes(wattPayload), 1, true);
                        }
                    }
                    case Fan -> {
                       topic = DISCOVERY_PREFIX + "/fan/" + baseId + "/config";
                        payload.put("default_entity_id", "fan." + objectId);
                        // 기본 전원 관련
                        payload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/power");
                        payload.put("command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/power");
                        payload.put("payload_on", "ON");
                        payload.put("payload_off", "OFF");

                        // 팬 모드(preset_mode)를 normal/bypass로 사용
                        payload.put("preset_mode_state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/mode");
                        payload.put("preset_mode_command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/power");
                        payload.put("preset_modes", List.of("NORMAL", "BYPASS"));

                        // 팬 속도는 percentage 기반으로 처리
                        payload.put("percentage_state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/speed");
                        payload.put("percentage_command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/speed");
                        payload.put("percentage_value_template", """
                          {% if value == 'LOW' %}
                            33
                          {% elif value == 'MEDIUM' %}
                            66
                          {% elif value == 'HIGH' %}
                            100
                          {% else %}
                            0
                          {% endif %}
                          """);

                        payload.put("percentage_command_template", """
                          {% set v = value | int %}
                          {% if v <= 49 %}
                            LOW
                          {% elif v <= 82 %}
                            MEDIUM
                          {% else %}
                            HIGH
                          {% endif %}
                        """);
                    }
                    case Thermo -> {
                        topic = DISCOVERY_PREFIX + "/climate/" + baseId + "/config";
                        payload.put("default_entity_id", "climate." + objectId);
                        payload.put("current_temperature_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/curTemp");
                        payload.put("temperature_state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/setTemp");
                        payload.put("temperature_command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/setTemp");
                        payload.put("mode_state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/mode");
                        payload.put("mode_command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/mode");
                        payload.put("action_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/action");
                        payload.put("modes", List.of("off", "heat"));
                        payload.put("temperature_unit", "C");
                        payload.put("min_temp", 5);
                        payload.put("max_temp", 40);
                        payload.put("temp_step", 1);
                    }
                    case Gas -> {
                        // button
                        topic = DISCOVERY_PREFIX + "/button/" + baseId + "/config";
                        payload.put("default_entity_id", "button." + objectId);
                        payload.put("command_topic", mqttProperties.getHaTopic() + "/command/" + baseId + "/button");
                        payload.put("payload_press", "PRESS");
                    
                        // binary_sensor
                        String binTopic = DISCOVERY_PREFIX + "/binary_sensor/" + baseId + "/config";
                        Map<String, Object> binPayload = new HashMap<>(payload);
                        binPayload.put("name", "가스밸브 " + device.getIndex());
                        binPayload.put("default_entity_id", "binary_sensor." + objectId + "_valve");
                        binPayload.put("unique_id", uniqueId + "_valve");
                        binPayload.put("state_topic", mqttProperties.getHaTopic() + "/state/" + baseId + "/power");
                        binPayload.put("payload_on", "ON");
                        binPayload.put("payload_off", "OFF");
                        mqttClient.publish(binTopic, objectMapper.writeValueAsBytes(binPayload), 1, true);
                    }
                    default -> log.warn("⚠️ 지원되지 않는 장치 타입: {}", device.getType());
                }

                if (topic != null) {
                    String json = objectMapper.writeValueAsString(payload);
                    mqttClient.publish(topic, json.getBytes(), 1, true);
                    log.info("📡 MQTT Discovery 등록: {}", topic);
                }

            } catch (Exception e) {
                log.error("❌ MQTT Discovery 등록 실패: {}", device.getUniqueId(), e);
            }
        }
    }
}
