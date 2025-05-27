from typing import Union

class StateUpdater:
    def __init__(self, ha_topic: str, publish_mqtt_func):
        self.STATE_TOPIC = ha_topic
        self.publish_mqtt = publish_mqtt_func

    async def update_light(self, idx: int, onoff: str) -> None:
        state = 'power'
        deviceID = 'Light' + str(idx)

        topic = self.STATE_TOPIC.format(deviceID, state)
        self.publish_mqtt(topic, onoff)
    
    async def update_light_breaker(self, idx: int, onoff: str) -> None:
        state = 'power'
        deviceID = 'LightBreaker' + str(idx)

        topic = self.STATE_TOPIC.format(deviceID, state)
        self.publish_mqtt(topic, onoff)

    async def update_temperature(self, idx: int, mode_text: str, action_text: str, curTemp: int, setTemp: int) -> None:
        """
        온도 조절기 상태를 업데이트하는 함수입니다.

        Args:
            idx (int): 온도 조절기 장치의 인덱스 번호.
            mode_text (str): 온도 조절기의 모드 텍스트 (예: 'heat', 'off').
            action_text (str): 온도 조절기의 동작 텍스트 (예: 'heating', 'idle').
            curTemp (int): 현재 온도 값.
            setTemp (int): 설정하고자 하는 목표 온도 값.

        Raises:
            Exception: 온도 업데이트 중 오류가 발생하면 예외를 발생시킵니다.
        """
        try:
            deviceID = 'Thermo' + str(idx)
            
            # 온도 상태 업데이트
            temperature = {
                'curTemp': str(curTemp).zfill(2),
                'setTemp': str(setTemp).zfill(2)
            }
            for state in temperature:
                val = temperature[state]
                topic = self.STATE_TOPIC.format(deviceID, state)
                self.publish_mqtt(topic, val)
            
            power_topic = self.STATE_TOPIC.format(deviceID, 'power')
            action_topic = self.STATE_TOPIC.format(deviceID, 'action')
            self.publish_mqtt(power_topic, mode_text)
            self.publish_mqtt(action_topic, action_text)
            
        except Exception as e:
            raise Exception(f"온도 업데이트 중 오류 발생: {str(e)}")
 
    async def update_fan(self, idx: int, power_text: str, speed_text: str) -> None:
        try:
            deviceID = 'Fan' + str(idx)
            if power_text == 'OFF':
                topic = self.STATE_TOPIC.format(deviceID, 'power')
                self.publish_mqtt(topic,'OFF')
            else:
                topic = self.STATE_TOPIC.format(deviceID, 'speed')
                self.publish_mqtt(topic, speed_text)
                topic = self.STATE_TOPIC.format(deviceID, 'power')
                self.publish_mqtt(topic, 'ON')
                
        except Exception as e:
            raise Exception(f"팬 상태 업데이트 중 오류 발생: {str(e)}")

    async def update_outlet(self,
                            idx: int, 
                            power_text: str, 
                            watt: Union[float,None], 
                            cutoff: Union[int,None], 
                            is_eco: Union[bool,None]) -> None:
        try:
            deviceID = 'Outlet' + str(idx)
            topic = self.STATE_TOPIC.format(deviceID, 'power')
            self.publish_mqtt(topic, power_text)
            if is_eco is not None:
                topic = self.STATE_TOPIC.format(deviceID, 'ecomode')
                self.publish_mqtt(topic, 'ON' if is_eco else 'OFF')
            if watt is not None:
                topic = self.STATE_TOPIC.format(deviceID, 'watt')
                self.publish_mqtt(topic, '%.1f' % watt)
            if cutoff is not None:
                topic = self.STATE_TOPIC.format(deviceID, 'cutoff')
                self.publish_mqtt(topic, str(cutoff))

        except Exception as e:
            raise Exception(f"콘센트 상태 업데이트 중 오류 발생: {str(e)}")

    async def update_gas(self, idx: int, power_text: str) -> None:
        try:
            deviceID = 'Gas' + str(idx)
            topic = self.STATE_TOPIC.format(deviceID, 'power')
            self.publish_mqtt(topic, power_text)
        except Exception as e:
            raise Exception(f"가스밸브 상태 업데이트 중 오류 발생: {str(e)}")

    async def update_ev(self, idx: int, power_text: str, floor_text: str) -> None:
        try:
            deviceID = 'EV' + str(idx)
            if power_text == 'ON':
                topic = self.STATE_TOPIC.format(deviceID, 'power')
                self.publish_mqtt(topic, 'ON')
                topic = self.STATE_TOPIC.format(deviceID, 'floor')
                self.publish_mqtt(topic, floor_text)
        except Exception as e:
            raise Exception(f"엘리베이터 상태 업데이트 중 오류 발생: {str(e)}")