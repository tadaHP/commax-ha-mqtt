package com.hyeonpyo.wallpadcontroller.ew11;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.mqtt.sender.MqttSendService;
import com.hyeonpyo.wallpadcontroller.properties.Ew11Properties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${ew11.transport:mqtt}'.equalsIgnoreCase('mqtt')")
public class MqttEw11Transport implements Ew11Transport {

    private final MqttSendService mqttSendService;
    private final Ew11Properties ew11Properties;

    @Override
    public void send(byte[] payload) {
        mqttSendService.publish(ew11Properties.getMqtt().getSendTopic(), payload, 0, false);
    }
}
