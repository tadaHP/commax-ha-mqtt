from typing import Any, Dict, List, Optional, TypedDict, Union
import re
from .utils import byte_to_hex_str, checksum

class ExpectedStatePacket(TypedDict):
    required_bytes: List[int]
    possible_values: List[List[str]]

class MessageProcessor:
    def __init__(self, controller: Any) -> None:
        self.controller = controller
        self.logger = controller.logger
        self.DEVICE_STRUCTURE = controller.DEVICE_STRUCTURE
        self.COLLECTDATA = controller.COLLECTDATA
        self.QUEUE = controller.QUEUE
        self.HA_TOPIC = controller.HA_TOPIC
        self.ELFIN_TOPIC = controller.ELFIN_TOPIC
        self.config = controller.config

    def make_climate_command(self, device_id: int, target_temp: int, command_type: str) -> Union[str, None]:
        """
        온도 조절기의 16진수 명령어를 생성하는 함수
        
        Args:
            device_id (int): 온도 조절기 장치 id
            current_temp (int): 현재 온도 값
            target_temp (int): 설정하고자 하는 목표 온도 값
            command_type (str): 명령어 타입
                - 'commandOFF': 전원 끄기 명령
                - 'commandON': 전원 켜기 명령
                - 'commandCHANGE': 온도 변경 명령
        
        Returns:
            Union[str, None]: 
                - 성공 시: 체크섬이 포함된 16진수 명령어 문자열
                - 실패 시: None
        
        Examples:
            >>> make_climate_command(0, 24, 'commandON')  # 온도절기 1번 켜기
            >>> make_climate_command(1, 26, 'commandCHANGE')  # 온도조절기 2번 온도 변경
        """
        try:
            assert isinstance(self.DEVICE_STRUCTURE, dict), "DEVICE_STRUCTURE must be a dictionary"
            
            thermo_structure = self.DEVICE_STRUCTURE["Thermo"]
            command = thermo_structure["command"]
            
            # 패킷 초기화
            packet = bytearray([0] * 7)

            # 헤더 설정
            packet[0] = int(command["header"], 16)
            
            # 기기 번호 설정 
            device_id_pos = command["fieldPositions"]["deviceId"]
            packet[int(device_id_pos)] = device_id
            
            # 명령 타입 및 값 설정
            command_type_pos = command["fieldPositions"]["commandType"]
            value_pos = command["fieldPositions"]["value"]
            
            if command_type == 'commandOFF':
                packet[int(command_type_pos)] = int(command["structure"][command_type_pos]["values"]["power"], 16)
                packet[int(value_pos)] = int(command["structure"][value_pos]["values"]["off"], 16)
            elif command_type == 'commandON':
                packet[int(command_type_pos)] = int(command["structure"][command_type_pos]["values"]["power"], 16)
                packet[int(value_pos)] = int(command["structure"][value_pos]["values"]["on"], 16)
            elif command_type == 'commandCHANGE':
                packet[int(command_type_pos)] = int(command["structure"][command_type_pos]["values"]["change"], 16)
                packet[int(value_pos)] = int(str(target_temp), 16)
            else:
                self.logger.error(f'온도조절기에 잘못된 명령 타입: {command_type}, 가능한 명령 타입: [commandOFF, commandON, commandCHANGE]')
                return None
            
            # 패킷을 16진수 문자열로 변환
            packet_hex = packet.hex().upper()
            
            # 체크섬 추가하여 return
            return checksum(packet_hex)
        
        except KeyError as e:
            # DEVICE_STRUCTURE에 필요한 키가 없는 경우
            self.logger.error(f'DEVICE_STRUCTURE에 필요한 키가 없습니다: {e}')
            return None
        except Exception as e:
            # 기타 예외 처리
            self.logger.error(f'예외 발생: {e}')
            return None

    def generate_expected_state_packet(self, command_str: str) -> Union[ExpectedStatePacket, None]:
        """명령 패킷으로부터 예상되는 상태 패킷을 생성합니다.
        
        Args:
            command_str (str): 16진수 형태의 명령 패킷 문자열
            
        Returns:
            Union[ExpectedStatePacket, None]: 예상되는 상태 패킷 정보를 담은 딕셔너리 또는 None
        """
        try:
            assert isinstance(self.DEVICE_STRUCTURE, dict)
            
            # 명령 패킷 검증
            if len(command_str) != 16:
                self.logger.error("예상패킷 생성 중 오류: 명령 패킷 길이가 16자가 아닙니다.")
                return None
                
            # 명령 패킷을 바이트로 변환
            command_packet = bytes.fromhex(command_str)

            #TODO: 8바이트 이외 패킷 처리 필요시 바이트 길이 판단 필요
            possible_values: List[List[str]] = [[] for _ in range(7)]

            # 헤더로 기기 타입 찾기
            device_type = None
            for name, structure in self.DEVICE_STRUCTURE.items():
                if command_packet[0] == int(structure['command']['header'], 16):
                    device_type = name
                    break
                    
            if not device_type:
                self.logger.error("예상패킷 생성 중 오류: 정의되지 않은 device type입니다.")
                return None
                        
            # 기기별 상태 패킷 생성
            device_structure = self.DEVICE_STRUCTURE[device_type]
            command_structure = device_structure['command']['structure']
            state_structure = device_structure['state']['structure']
            command_field_positions = device_structure['command']['fieldPositions']
            state_field_positions = device_structure['state']['fieldPositions']
            
            # 필요한 바이트 리스트
            required_bytes = [0] # 헤더는 항상 포함
            possible_values[0] = [device_structure['state']['header']]
            
            # 기기 ID
            if 'deviceId' in state_field_positions:
                device_id_pos = state_field_positions['deviceId']
                required_bytes.append(int(device_id_pos))
                possible_values[int(device_id_pos)] = [byte_to_hex_str(command_packet[int(command_field_positions.get('deviceId', 1))])]

            if device_type == 'Thermo':
                # 온도조절기 상태 패킷 생성
                command_type_pos = command_field_positions.get('commandType', 2)
                command_type = command_packet[int(command_type_pos)]
                value_pos = command_field_positions.get('value',3)
                
                power_pos = state_field_positions.get('power',1)
                if command_type == int(command_structure[str(command_type_pos)]['values']['power'], 16): #04
                    command_value = command_packet[int(value_pos)] #command value on:81, off:00
                    #off인경우
                    if command_value == int(command_structure[str(value_pos)]['values']['off'], 16):
                        possible_values[int(power_pos)] = [state_structure[str(power_pos)]['values']['off']]
                    #off가 아닌경우 (on)
                    else:
                        possible_values[int(power_pos)] = [
                            state_structure[str(power_pos)]['values']['idle'],
                            state_structure[str(power_pos)]['values']['heating']
                        ]
                    required_bytes.append(int(power_pos))

                elif command_type == int(command_structure[str(command_type_pos)]['values']['change'], 16): #03
                    target_temp = command_packet[int(value_pos)]

                    target_temp_pos = state_field_positions.get('targetTemp', 4)

                    # 필요한 바이트 리스트에 목표 온도 위치 추가
                    required_bytes.append(int(target_temp_pos))
                    possible_values[int(target_temp_pos)] = [byte_to_hex_str(target_temp)]
            
            #on off 타입 기기
            elif device_type == 'Light' or device_type == 'LightBreaker' or device_type == 'Gas':
                state_power_pos = state_field_positions.get('power',1)
                command_power_pos = command_field_positions.get('power',2)
                command_power_value = command_packet[int(command_power_pos)]
                #off인 경우
                if command_power_value == int(command_structure[str(command_power_pos)]['values']['off'], 16):
                    possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['off']]
                #on인 경우
                else:
                    possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['on']]
                # 필요한 바이트 리스트에 전원 위치 추가
                required_bytes.append(int(state_power_pos))
                
            elif device_type == 'Outlet':
                command_type_pos = command_field_positions.get('commandType', 2)
                command_type = command_packet[int(command_type_pos)]

                state_power_pos = state_field_positions.get('power',1)
                command_power_pos = command_field_positions.get('power',2)
                command_power_value = command_packet[int(command_power_pos)]

                if command_type == int(command_structure[command_type_pos]['values']['power'], 16):
                    #power off인경우
                    if command_power_value == int(command_structure[str(command_field_positions.get('power', 3))]['values']['off'], 16):
                        # off with or without eco
                        possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['off'],state_structure[str(state_power_pos)]['values']['off_with_eco']]
                    else:
                        # on with or without eco
                        possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['on'],state_structure[str(state_power_pos)]['values']['on_with_eco']]
                    required_bytes.append(int(state_power_pos))
                elif command_type == int(command_structure[command_type_pos]['values']['ecomode'], 16):
                    #eco off인경우
                    if command_power_value == int(command_structure[str(command_field_positions.get('power', 3))]['values']['off'], 16):
                        # on or off without eco
                        possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['on'], state_structure[str(state_power_pos)]['values']['off']]
                    else:
                        # on or off with eco
                        possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['on_with_eco'], state_structure[str(state_power_pos)]['values']['off_with_eco']]
                    required_bytes.append(int(state_power_pos))
                elif command_type == int(command_structure[command_type_pos]['values']['setCutoff'], 16):
                    #setCutoff인경우
                    command_cutoffvalue_pos = command_field_positions.get('cutoffValue',4)
                    state_type_pos = state_field_positions.get('stateType',3)
                    possible_values[int(state_type_pos)] = [state_structure[str(state_type_pos)]['values']['ecomode']]
                    required_bytes.append(int(state_type_pos))

                    state_cutoffvalue_pos = state_field_positions.get('data3',6)
                    possible_values[int(state_cutoffvalue_pos)] = [byte_to_hex_str(command_packet[int(command_cutoffvalue_pos)])]
                    required_bytes.append(int(state_cutoffvalue_pos))

            elif device_type == 'Fan':
                # 팬 상태 패킷 생성
                command_type_pos = command_field_positions.get('commandType', 2)
                command_type = command_packet[int(command_type_pos)]
                
                state_power_pos = state_field_positions.get('power',1)
                if command_type == int(command_structure[command_type_pos]['values']['power'], 16):
                    command_value = command_packet[int(command_field_positions.get('value', 3))]
                    #off인경우
                    if command_value == int(command_structure[str(command_field_positions.get('value', 3))]['values']['off'], 16):
                        possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['off']]
                    #off가 아닌경우 (on)
                    else:
                        possible_values[int(state_power_pos)] = [state_structure[str(state_power_pos)]['values']['on']]
                    required_bytes.append(int(state_power_pos))

                elif command_type == int(command_structure[command_type_pos]['values']['setSpeed'], 16):
                    speed = command_packet[int(command_field_positions.get('value', 3))]
                    state_speed_pos = state_field_positions.get('speed', 4)
                    required_bytes.append(int(state_speed_pos))
                    possible_values[int(state_speed_pos)] = [state_structure[str(state_speed_pos)]['values'][str(speed)]]
            
            return ExpectedStatePacket(
                required_bytes=sorted(required_bytes),
                possible_values=possible_values
            )
            
        except Exception as e:
            self.logger.error(f"상태 패킷 생성 중 오류 발생: {str(e)}\n"
                            f"장치 타입: {device_type}\n"
                            f"명령 패킷: {command_packet.hex().upper()}\n"
                            f'required_bytes: {required_bytes}\n'
                            f'possible_values: {possible_values}\n'
                            f"State_structure: {state_structure}\n"
                            f"command_structure: {command_structure}")
            return None

    async def process_elfin_data(self, raw_data: str) -> None:
        """Elfin 장치에서 전송된 raw_data를 분석합니다."""
        try:
            assert isinstance(self.DEVICE_STRUCTURE, dict), "DEVICE_STRUCTURE must be a dictionary"
            
            for k in range(0, len(raw_data), 16):
                data = raw_data[k:k + 16]
                if data == checksum(data):
                    self.COLLECTDATA['recv_data'].append(data)
                    self.COLLECTDATA['recent_recv_data'].add(data)
                    if len(self.COLLECTDATA['recv_data']) > 300:
                        self.COLLECTDATA['recv_data'] = self.COLLECTDATA['recv_data'][-300:]
                    
                    byte_data = bytearray.fromhex(data)
                    
                    for device_name, structure in self.DEVICE_STRUCTURE.items():
                        state_structure = structure['state']
                        field_positions = state_structure['fieldPositions']
                        if byte_data[0] == int(state_structure['header'], 16):
                            try:
                                device_id_pos = field_positions['deviceId']
                                device_id = byte_data[int(device_id_pos)]
                            except KeyError:
                                # Gas같은 deviceId가 없는 기기 처리 여기에..
                                if device_name == 'Gas':
                                    power_pos = field_positions.get('power', 1)
                                    power = byte_data[int(power_pos)]
                                    power_hex = byte_to_hex_str(power)
                                    power_values = state_structure['structure'][power_pos]['values']
                                    power_text = "ON" if power_hex == power_values.get('on', '').upper() else "OFF"
                                    self.logger.signal(f'{byte_data.hex()}: 가스차단기 ### 상태: {power_text}')
                                    await self.controller.state_updater.update_gas(1, power_text) #deviceId 항상 1
                                break
                            except IndexError:
                                self.logger.error(f"{device_name}의 deviceId 위치({device_id_pos})가 패킷 범위를 벗어났습니다.")
                                break
                            if device_name == 'Thermo':
                                power_pos = field_positions.get('power', 1)
                                power = byte_data[int(power_pos)]
                                # 온도값을 10진수로 직접 해석
                                current_temp = int(format(byte_data[int(field_positions.get('currentTemp', 3))], '02x'))
                                target_temp = int(format(byte_data[int(field_positions.get('targetTemp', 4))], '02x'))
                                power_hex = byte_to_hex_str(power)
                                power_values = state_structure['structure'][power_pos]['values']
                                power_off_hex = power_values.get('off', '').upper()
                                power_heating_hex = power_values.get('heating', '').upper()
                                mode_text = 'off' if power_hex == power_off_hex else 'heat'
                                action_text = 'heating' if power_hex == power_heating_hex else 'idle'
                                self.logger.signal(f'{byte_data.hex()}: 온도조절기 ### {device_id}번, 모드: {mode_text}, 현재 온도: {current_temp}°C, 설정 온도: {target_temp}°C')
                                await self.controller.state_updater.update_temperature(device_id, mode_text, action_text, current_temp, target_temp)
                            
                            elif device_name == 'Light':
                                power_pos = field_positions.get('power', 1)
                                power = byte_data[int(power_pos)]
                                power_values = state_structure['structure'][power_pos]['values']
                                power_hex = byte_to_hex_str(power)
                                state = "ON" if power_hex == power_values.get('on', '').upper() else "OFF"
                                
                                self.logger.signal(f'{byte_data.hex()}: 조명 ### {device_id}번, 상태: {state}')
                                await self.controller.state_updater.update_light(device_id, state)

                            elif device_name == 'LightBreaker':
                                power_pos = field_positions.get('power', 1)
                                power = byte_data[int(power_pos)]
                                power_values = state_structure['structure'][power_pos]['values']
                                power_hex = byte_to_hex_str(power)
                                state = "ON" if power_hex == power_values.get('on', '').upper() else "OFF"
                                
                                self.logger.signal(f'{byte_data.hex()}: 조명차단기 ### {device_id}번, 상태: {state}')
                                await self.controller.state_updater.update_light_breaker(device_id, state)
                                
                            elif device_name == 'Outlet':
                                power_pos = field_positions.get('power', 1)
                                power = byte_data[int(power_pos)]
                                power_values = state_structure['structure'][power_pos]['values']
                                power_hex = byte_to_hex_str(power)
                                power_text = "ON" if power_hex in [power_values.get('on', '').upper(), power_values.get('on_with_eco', '').upper()] else "OFF"
                                is_eco = True if power_hex in [power_values.get('on_with_eco', '').upper(), power_values.get('off_with_eco', '').upper()] else False
                                state_type_pos = field_positions.get('stateType', 3)
                                state_type = byte_data[int(state_type_pos)]
                                state_type_values = state_structure['structure'][state_type_pos]['values']
                                state_type_hex = byte_to_hex_str(state_type)
                                state_type_text = 'wattage' if state_type_hex in [state_type_values.get('wattage','')] else 'ecomode'

                                try:
                                    wattage_scailing_factor = float(state_structure.get("wattage_scailing_factor", 0.1))
                                    if wattage_scailing_factor == 0:
                                        self.logger.warning("outlet의 wattage scailing factor가 0으로 해석되고있습니다. 기본값인 0.1로 대체합니다.")
                                        wattage_scailing_factor = 0.1
                                except (ValueError, TypeError):
                                    self.logger.warning("outlet의 wattage scailing factor를 해석할 수 없습니다. 기본값인 0.1로 대체합니다.")
                                    wattage_scailing_factor = 0.1
                                try:
                                    ecomode_scailing_factor = float(state_structure.get("ecomode_scailing_factor", 1))
                                    if ecomode_scailing_factor == 0:
                                        self.logger.warning("outlet의 ecomode scailing factor가 0으로 해석되고있습니다. 기본값인 1로 대체합니다.")
                                        ecomode_scailing_factor = 1
                                except (ValueError, TypeError):
                                    self.logger.warning("outlet의 ecomode scailing factor를 해석할 수 없습니다. 기본값인 1로 대체합니다.")
                                    ecomode_scailing_factor = 1

                                consecutive_bytes = byte_data[4:7]
                                try:
                                    watt = int(consecutive_bytes.hex())
                                except ValueError:
                                    self.logger.error(f"콘센트 {device_id} 전력값/자동대기전력차단값 변환 중 오류 발생: {consecutive_bytes.hex()}")
                                    watt = 0
                                    
                                if state_type_text == 'wattage':
                                    self.logger.signal(f'{byte_data.hex()}: 콘센트 ### {device_id}번, 상태: {power_text}, 전력: {watt} x {wattage_scailing_factor}W')
                                    await self.controller.state_updater.update_outlet(device_id, power_text, watt * wattage_scailing_factor, None, is_eco)
                                elif state_type_text == 'ecomode':
                                    self.logger.signal(f'{byte_data.hex()}: 콘센트 ### {device_id}번, 상태: {power_text}, 자동대기전력차단값: {watt} x {ecomode_scailing_factor} W')
                                    await self.controller.state_updater.update_outlet(device_id, power_text, None, watt * ecomode_scailing_factor, is_eco)

                            elif device_name == 'Fan':
                                power_pos = field_positions.get('power', 1)
                                power = byte_data[int(power_pos)]
                                power_values = state_structure['structure'][power_pos]['values']
                                power_hex = byte_to_hex_str(power)
                                power_text = "OFF" if power_hex == power_values.get('off', '').upper() else "ON"
                                speed_pos = field_positions.get('speed', 3)  
                                speed = byte_data[int(speed_pos)]
                                speed_values = state_structure['structure'][speed_pos]['values']
                                speed_hex = byte_to_hex_str(speed)
                                speed_text = speed_values.get(speed_hex, 'low')
                                
                                self.logger.signal(f'{byte_data.hex()}: 환기장치 ### {device_id}번, 상태: {power_text}, 속도: {speed_text}')
                                await self.controller.state_updater.update_fan(device_id, power_text, speed_text)
                            
                            elif device_name == 'EV':
                                power_pos = field_positions.get('power', 1)
                                power = byte_data[int(power_pos)]
                                power_values = state_structure['structure'][power_pos]['values']
                                power_hex = byte_to_hex_str(power)
                                power_text = "ON" if power_hex == power_values.get('on', '').upper() else "OFF"
                                floor_pos = field_positions.get('floor', 3)
                                floor = byte_data[int(floor_pos)]
                                floor_hex = byte_to_hex_str(floor)
                                self.logger.signal(f'{byte_data.hex()}: 엘리베이터 ### {device_id}번, 상태: {power_text}, 층: {floor_hex}')
                                await self.controller.state_updater.update_ev(device_id, power_text, floor_hex)

                            break
                else:
                    self.logger.signal(f'체크섬 불일치: {data}')
        
        except Exception as e:
            self.logger.error(f"Elfin 데이터 처리 중 오류 발생: {str(e)}")
            self.logger.debug(f"오류 상세 - raw_data: {raw_data}, device_name: {device_name if 'device_name' in locals() else 'N/A'}")

    async def process_ha_command(self, topics: List[str], value: str) -> None:
        try:
            device = ''.join(re.findall('[a-zA-Z]', topics[1]))
            device_id = int(''.join(re.findall('[0-9]', topics[1])))
            action = topics[2]

            assert isinstance(self.DEVICE_STRUCTURE, dict), "DEVICE_STRUCTURE must be a dictionary"

            if device not in self.DEVICE_STRUCTURE:
                self.logger.error(f'장치 {device}가 DEVICE_STRUCTURE에 존재하지 않습니다.')
                return

            packet_hex = None
            packet = bytearray(7)
            device_structure = self.DEVICE_STRUCTURE[device]
            command = device_structure["command"]
            field_positions = command["fieldPositions"]
            
            packet[0] = int(device_structure["command"]["header"], 16)
            packet[int(field_positions["deviceId"])] = device_id

            if device == 'Light':
                if action == 'power':
                    power_value = command["structure"][str(field_positions["power"])]["values"]["on" if value == "ON" else "off"]
                    packet[int(field_positions["power"])] = int(power_value, 16)
                    self.logger.info(f'조명 {device_id} {action} {value} 명령 생성 {packet.hex().upper()}')
                #TODO: dimmer 추가
            elif device == 'LightBreaker':
                command_type_value = command["structure"][str(field_positions["commandType"])]["values"]["power"]
                packet[int(field_positions["commandType"])] = int(command_type_value, 16)
                power_value = command["structure"][str(field_positions["power"])]["values"]["on" if value == "ON" else "off"]
                packet[int(field_positions["power"])] = int(power_value, 16)
                self.logger.info(f'조명차단기 {device_id} {action} {value} 명령 생성 {packet.hex().upper()}')
            elif device == 'Outlet':
                if action == 'power':
                    command_type_value = command["structure"][str(field_positions["commandType"])]["values"]["power"]
                    packet[int(field_positions["commandType"])] = int(command_type_value, 16)
                    power_value = command["structure"][str(field_positions["power"])]["values"]["on" if value == "ON" else "off"]
                    packet[int(field_positions["power"])] = int(power_value, 16)
                    self.logger.info(f'콘센트 {device_id} {action} {value} 명령 생성 {packet.hex().upper()}')
                elif action == 'ecomode':
                    command_type_value = command["structure"][str(field_positions["commandType"])]["values"]["ecomode"]
                    packet[int(field_positions["commandType"])] = int(command_type_value, 16)
                    power_value = command["structure"][str(field_positions["power"])]["values"]["on" if value == "ON" else "off"]
                    packet[int(field_positions["power"])] = int(power_value, 16)
                    self.logger.info(f'콘센트 {device_id} {action} {value} 명령 생성 {packet.hex().upper()}')
                elif action == 'setCutoff':
                    command_type_value = command["structure"][str(field_positions["commandType"])]["values"]["setCutoff"]
                    packet[int(field_positions["commandType"])] = int(command_type_value, 16)
                    packet[int(field_positions["cutoffValue"])] = int(value, 16)
                    self.logger.info(f'콘센트 {device_id} {action} {value} 명령 생성 {packet.hex().upper()}')
            elif device == 'Gas':
                # 가스밸브 차단 명령
                if value == "PRESS" or value == "ON":
                    power_value = command["structure"][str(field_positions["power"])]["values"]["off"]
                    packet[int(field_positions["power"])] = int(power_value, 16)
                    self.logger.info(f'가스차단기 {device_id} 차단 명령 생성 {packet.hex().upper()}')
            elif device == 'Thermo':                
                if action == 'power':
                    if value == 'heat':
                        packet_hex = self.make_climate_command(device_id, 0, 'commandON')
                    else:
                        packet_hex = self.make_climate_command(device_id, 0, 'commandOFF')
                elif action == 'setTemp':
                    try:
                        set_temp = int(float(value))
                        min_temp = int(self.config['climate_settings'].get('min_temp', 5))
                        max_temp = int(self.config['climate_settings'].get('max_temp', 40))
                        
                        if not min_temp <= set_temp <= max_temp:
                            self.logger.error(f"설정 온도가 허용 범위를 벗어났습니다: {set_temp}°C (허용범위: {min_temp}~{max_temp}°C)")
                            return
                    except ValueError as e:
                        self.logger.error(f"온도 값이 올바르지 않습니다: {value}")
                        return
                    packet_hex = self.make_climate_command(device_id, set_temp, 'commandCHANGE')
                self.logger.info(f'온도조절기 {device_id} {action} {value} 명령 생성 {packet_hex}')
            elif device == 'Fan':
                if action == 'power':
                    command_type_value = command["structure"][str(field_positions["commandType"])]["values"]["power"]
                    packet[int(field_positions["commandType"])] = int(command_type_value, 16)
                    power_value = command["structure"][str(field_positions["value"])]["values"]["on" if value == "ON" else "off"]
                    packet[int(field_positions["value"])] = int(power_value, 16)
                elif action == 'speed':
                    if value not in ["low", "medium", "high"]:
                        self.logger.error(f"잘못된 팬 속도입니다: {value}")
                        return
                    command_type_value = command["structure"][str(field_positions["commandType"])]["values"]["setSpeed"]
                    packet[int(field_positions["commandType"])] = int(command_type_value, 16)
                    value_value = command["structure"][str(field_positions["value"])]["values"][value]
                    packet[int(field_positions["value"])] = int(value_value, 16)
                self.logger.info(f'환기장치 {device_id} {action} {value} 명령 생성 {packet.hex().upper()}')
            elif device == 'EV':
                # 엘리베이터 호출 명령
                if value == "PRESS" or value == "ON":
                    # EV 헤더 A0가 중복이라 따로 처리함..
                    packet[0] = int("A0", 16)
                    packet[int(field_positions["power"])] = int(command["structure"][str(field_positions["power"])]["values"]["on"], 16)
                    packet[int(field_positions["unknown1"])] = int(command["structure"][str(field_positions["unknown1"])]["values"]["fixed"], 16)
                    packet[int(field_positions["unknown2"])] = int(command["structure"][str(field_positions["unknown2"])]["values"]["fixed"], 16)
                    packet[int(field_positions["unknown3"])] = int(command["structure"][str(field_positions["unknown3"])]["values"]["fixed"], 16)
                    self.logger.info(f'엘리베이터 {device_id} 호출 명령 생성 {packet.hex().upper()}')

            if packet_hex is None:
                packet_hex = packet.hex().upper()
                packet_hex = checksum(packet_hex)

            if packet_hex:
                expected_state = self.generate_expected_state_packet(packet_hex)
                if expected_state:
                    self.logger.debug(f'예상 상태 패킷: {expected_state}')
                    self.QUEUE.append({
                        'sendcmd': packet_hex, 
                        'count': 0, 
                        'expected_state': expected_state,
                        'received_count': 0
                    })
                else:
                    self.logger.debug('예상 상태 패킷 없음. 최대 전송 횟수만큼 전송합니다.')
                    self.QUEUE.append({
                        'sendcmd': packet_hex, 
                        'count': 0, 
                        'expected_state': None,
                        'received_count': 0
                    })
        except Exception as e:
            self.logger.error(f"HA 명령 처리 중 오류 발생: {str(e)}") 