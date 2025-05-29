package com.hyeonpyo.wallpadcontroller.mqtt.receive;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.elfin.ElfinReceiveService;
import com.hyeonpyo.wallpadcontroller.properties.MqttProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttReceiveService implements MqttCallback{

    private final MqttProperties mqttProperties;
    private final String EW11_RECEIVE_TOPIC = "ew11/recv";
    private final ElfinReceiveService elfinReceiveService;
    

    private final MqttClient mqttClient;

    @PostConstruct
    public void init() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.setCallback(this);
                log.info("📞 MQTT 콜백 설정 완료: {}", this.getClass().getSimpleName());

                //토픽 구독
                mqttClient.subscribe(EW11_RECEIVE_TOPIC, 0);
                log.info("📥 MQTT 구독 완료: {}", EW11_RECEIVE_TOPIC);

            } else {
                log.error("⚠️ MQTT 클라이언트가 연결되어 있지 않아, MqttReceiveService 초기화(콜백 설정 및 구독)를 진행할 수 없습니다.");
            }
        } catch (MqttException e) {
            log.error("❌ MqttReceiveService 초기화 중 MQTT 오류 발생 (콜백 설정 또는 구독 실패)", e);
        } catch (Exception e) {
            log.error("❌ MqttReceiveService 초기화 중 알 수 없는 오류 발생", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("⚠️ MQTT 연결 끊김: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {

        
        switch (topic) {
            case EW11_RECEIVE_TOPIC:
                elfinReceiveService.publishCommax(message);
                break;
            default:
                break;
        }


        
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            if (token != null && token.getMessage() != null) {
                log.debug("🚚 MQTT 메시지 전달 완료 (토큰 ID: {}, 메시지 ID: {})", token.hashCode(), token.getMessage().getId());
            } else if (token != null) {
                log.debug("🚚 MQTT 메시지 전달 완료 (토큰 ID: {})", token.hashCode());
            }
        } catch (MqttException e) {
             log.warn("deliveryComplete 콜백에서 메시지 정보 조회 중 오류", e);
        }
    }
}
