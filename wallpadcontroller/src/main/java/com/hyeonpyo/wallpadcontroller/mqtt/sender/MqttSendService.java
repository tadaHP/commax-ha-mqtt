package com.hyeonpyo.wallpadcontroller.mqtt.sender;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSendService {

    private final MqttClient mqttClient;

    /**
     * 지정된 토픽으로 메시지를 발행합니다.
     *
     * @param topic   발행할 토픽
     * @param payload 발행할 메시지 내용 (문자열)
     * @param qos     서비스 품질 (0, 1, 또는 2)
     */
    public void publish(String topic, String payload, int qos) {
        this.publish(topic, payload, qos, false);
    }

    /**
     * 지정된 토픽으로 메시지를 발행합니다 (retained 플래그 포함).
     *
     * @param topic    발행할 토픽
     * @param payload  발행할 메시지 내용 (문자열)
     * @param qos      서비스 품질 (0, 1, 또는 2)
     * @param retained 메시지를 브로커에 유지할지 여부
     */
    public void publish(String topic, String payload, int qos, boolean retained) {
        if (topic == null || topic.trim().isEmpty()) {
            log.warn("⚠️ MQTT 발행 실패: 토픽이 null이거나 비어있습니다.");
            return;
        }
        if (payload == null) {
            log.warn("⚠️ MQTT 발행 실패: 페이로드가 null입니다. (Topic: {})", topic);
            return;
        }

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                // MqttMessage 객체 생성. 페이로드는 byte[] 형태로 변환합니다.
                // 일반적으로 UTF-8 인코딩을 사용합니다.
                MqttMessage message = new MqttMessage(payload.getBytes("UTF-8"));
                message.setQos(qos);
                message.setRetained(retained); // Retained 메시지 설정

                mqttClient.publish(topic, message);
                // log.info("📤 MQTT 발행 성공: Topic: \"{}\", QoS: {}, Retained: {}, Payload: \"{}\"", topic, qos, retained, payload);

            } else {
                log.warn("⚠️ MQTT 클라이언트가 연결되어 있지 않습니다. 발행 시도 실패 - Topic: \"{}\", Payload: \"{}\"", topic, payload);
                // TODO:연결되지 않았을 때 메시지를 큐에 저장했다가 재연결 후 발행하는 로직을 추가할 수 있습니다.
            }
        } catch (MqttException e) {
            log.error("❌ MQTT 발행 중 오류 발생: Topic: \"{}\", Payload: \"{}\"", topic, payload, e);
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("❌ MQTT 발행 실패: 페이로드 인코딩 오류 (UTF-8 지원되지 않음) - Topic: \"{}\"", topic, e);
        } catch (Exception e) {
            log.error("❌ MQTT 발행 중 알 수 없는 오류 발생: Topic: \"{}\", Payload: \"{}\"", topic, payload, e);
        }
    }
}