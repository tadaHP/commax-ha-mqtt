from gevent import monkey; monkey.patch_all()  # type: ignore

import paho.mqtt.client as mqtt # type: ignore
import time
import asyncio
import os
from .logger import Logger
from typing import Any, Dict, Union, List, Optional, Set, TypedDict, Callable, TypeVar, Callable
from functools import wraps
import yaml # type: ignore #PyYAML
import json
import re
import telnetlib3 # type: ignore
import shutil
from .web_server import WebServer
from .utils import byte_to_hex_str, checksum
from .supervisor_api import SupervisorAPI
from .message_processor import MessageProcessor
from .discovery_publisher import DiscoveryPublisher
from .state_updater import StateUpdater

T = TypeVar('T')

def require_device_structure(default_return: Any = None) -> Callable:
    """DEVICE_STRUCTURE가 초기화되었는지 확인하는 데코레이터
    
    Args:
        default_return: DEVICE_STRUCTURE가 None일 때 반환할 기본값
        
    Returns:
        Callable: 데코레이터 함수
    """
    def decorator(func: Callable[..., T]) -> Callable[..., T]:
        @wraps(func)
        def wrapper(self, *args, **kwargs):
            if self.DEVICE_STRUCTURE is None:
                self.logger.error("DEVICE_STRUCTURE가 초기화되지 않았습니다.")
                return default_return
            return func(self, *args, **kwargs)
        return wrapper
    return decorator

class CollectData(TypedDict):
    send_data: List[str]
    recv_data: List[str]
    recent_recv_data: Set[str]
    last_recv_time: int

class ExpectedStatePacket(TypedDict):
    # expected_packet: str
    required_bytes: List[int]
    possible_values: List[List[str]]
    
class QueueItem(TypedDict):
    sendcmd: str
    count: int
    expected_state: Optional[ExpectedStatePacket]
    received_count: int  # 예상 패킷 수신 횟수를 추적하기 위한 필드

    
class WallpadController:
    def __init__(self, config: Dict[str, Any], logger: Logger) -> None:
        self.supervisor_api = SupervisorAPI()
        self.config: Dict[str, Any] = config
        self.logger: Logger = logger
        # self.share_dir: str = '/share'
        self.share_dir: str = './share'
        self.ELFIN_TOPIC: str = config.get('elfin_TOPIC','ew11')
        self.HA_TOPIC: str = config.get('mqtt_TOPIC','commax')
        self.STATE_TOPIC: str = self.HA_TOPIC + '/{}/{}/state'
        self.MQTT_HOST: str = self.config['mqtt'].get('mqtt_server') or os.getenv('MQTT_HOST') or "core-mosquitto"
        self.MQTT_PORT: int = int(self.config['mqtt'].get('mqtt_port') or os.getenv('MQTT_PORT') or 1883)
        self.MQTT_USER: str = os.getenv('MQTT_USER') or "my_user"
        self.MQTT_PASSWORD: str = os.getenv('MQTT_PASSWORD') or "m1o@s#quitto"
        self.QUEUE: List[QueueItem] = []
        self.max_send_count: int = self.config['command_settings'].get('max_send_count', 20)
        self.min_receive_count: int = self.config['command_settings'].get('min_receive_count', 3)  # 최소 수신 횟수, 기본값 3
        self.COLLECTDATA: CollectData = {
            'send_data': [],
            'recv_data': [],
            'recent_recv_data': set(),
            'last_recv_time': time.time_ns()
        }
        self.mqtt_client: Optional[mqtt.Client] = None
        self.device_list: Optional[Dict[str, Any]] = None
        self.DEVICE_STRUCTURE: Optional[Dict[str, Any]] = None
        self.loop: Optional[asyncio.AbstractEventLoop] = None
        self.load_devices_and_packets_structures() # 기기 정보와 패킷 정보를 로드 to self.DEVICE_STRUCTURE
        self.web_server = WebServer(self)
        self.elfin_reboot_count: int = 0
        self.elfin_unavailable_notification_enabled: bool = self.config['elfin'].get('elfin_unavailable_notification', False)
        self.send_command_on_idle: bool = self.config['command_settings'].get('send_command_on_idle', True)
        self.message_processor = MessageProcessor(self)
        self.discovery_publisher = DiscoveryPublisher(self)
        self.state_updater = StateUpdater(self.STATE_TOPIC, self.publish_mqtt)
        self.is_available: bool = False

    def load_devices_and_packets_structures(self) -> None:
        """
        기기 및 패킷 구조를 로드하는 함수
        
        config의 vendor 설정에 따라 기본 구조 파일 또는 커스텀 구조 파일을 로드합니다.
        vendor가 설정되지 않은 경우 기본값으로 'commax'를 사용합니다.
        """
        try:
            vendor = self.config.get('vendor', 'commax').lower()

            base_dir = os.path.dirname(os.path.abspath(__file__))
            
            # 기본 파일 경로 설정 (config에서 지정된 경로가 있으면 그것을 사용 for pytest)
            if 'packet_file' in self.config:
                default_file_path = self.config['packet_file']
            else:
                default_file_path = os.path.join(base_dir, 'packet_structures_commax.yaml')
                
            custom_file_path = os.path.join(base_dir, '..', 'share', 'packet_structures_custom.yaml')

            if vendor == 'custom':
                try:
                    with open(custom_file_path, 'r', encoding='utf-8') as file:
                        self.DEVICE_STRUCTURE = yaml.safe_load(file)
                    self.logger.info(f'{vendor} 패킷 구조를 로드했습니다.')
                except FileNotFoundError:
                    self.logger.info(f'{custom_file_path} 파일이 없습니다. 기본 파일을 복사합니다.')
                    try:
                        # share 디렉토리가 없으면 생성
                        os.makedirs(os.path.dirname(custom_file_path), exist_ok=True)
                        # default 파일을 custom 경로로 복사
                        shutil.copy(default_file_path, custom_file_path)
                        with open(custom_file_path, 'r', encoding='utf-8') as file:
                            self.DEVICE_STRUCTURE = yaml.safe_load(file)
                        self.logger.info(f'기본 패킷 구조를 {custom_file_path}로 복사하고 로드했습니다.')
                    except Exception as e:
                        self.logger.error(f'기본 파일 복사 중 오류 발생: {str(e)}')
            else:
                try:
                    with open(default_file_path, 'r', encoding='utf-8') as file:
                        self.DEVICE_STRUCTURE = yaml.safe_load(file)
                    self.logger.info(f'{vendor} 패킷 구조를 로드했습니다.')
                except FileNotFoundError:
                    self.logger.error(f'{vendor} 패킷 구조 파일을 찾을 수 없습니다.')
                    return

            if self.DEVICE_STRUCTURE is not None:
                # fieldPositions 자동 생성
                for device_name, device in self.DEVICE_STRUCTURE.items():
                    for packet_type in ['command', 'state']:
                        if packet_type in device:
                            structure = device[packet_type]['structure']
                            field_positions = {}
                            
                            for pos, field in structure.items():
                                field_name = field['name']
                                if field_name != 'empty':
                                    if field_name in field_positions:
                                        self.logger.error(
                                            f"중복된 필드 이름 발견: {device_name}.{packet_type} - "
                                            f"'{field_name}' (위치: {field_positions[field_name]}, {pos})"
                                        )
                                    else:
                                        field_positions[field_name] = pos
                                    
                            device[packet_type]['fieldPositions'] = field_positions
                        
        except FileNotFoundError:
            self.logger.error('기기 및 패킷 구조 파일을 찾을 수 없습니다.')
        except yaml.YAMLError:
            self.logger.error('기기 및 패킷 구조 파일의 YAML 형식이 잘못되었습니다.')

    # MQTT 관련 함수들
    def setup_mqtt(self, client_id: Optional[str] = None) -> mqtt.Client:
        """MQTT 클라이언트를 설정하고 반환합니다.
        
        Args:
            client_id (Optional[str]): MQTT 클라이언트 ID. 기본값은 self.HA_TOPIC
            
        Returns:
            mqtt.Client: 설정된 MQTT 클라이언트
        """
        try:
            client = mqtt.Client(client_id or self.HA_TOPIC)
            if self.config['mqtt'].get('mqtt_server'):
                self.logger.debug(f"MQTT User ({self.config['mqtt']['mqtt_id']})로 로그인")
                client.username_pw_set(
                    self.config['mqtt']['mqtt_id'],
                    self.config['mqtt']['mqtt_password']
                )
            else:
                self.logger.debug(f"기본 MQTT User ({self.MQTT_USER})로 로그인")
                client.username_pw_set(self.MQTT_USER, self.MQTT_PASSWORD)
            return client
            
        except Exception as e:
            self.logger.error(f"MQTT 클라이언트 설정 중 오류 발생: {str(e)}")
            raise

    def connect_mqtt(self) -> None:
        """MQTT 브로커에 최초 연결을 시도합니다."""
        if self.mqtt_client and self.mqtt_client.is_connected():
            self.logger.info("기존 MQTT 연결을 종료합니다.")
            self.mqtt_client.disconnect()
        try:
            self.logger.info("MQTT 브로커 연결 시도 중...")
            if self.mqtt_client:
                self.logger.debug(f"MQTT 호스트: {self.MQTT_HOST}")
                self.mqtt_client.will_set(f"{self.HA_TOPIC}/status", "offline", retain=True)
                self.mqtt_client.connect(self.MQTT_HOST, self.MQTT_PORT)
            else:
                self.logger.error("MQTT 클라이언트가 초기화되지 않았습니다.")
            return
        except Exception as e:
            self.logger.error(f"MQTT 연결 실패: {str(e)}")
            return

    def reconnect_mqtt(self) -> None:
        """MQTT 브로커 연결이 끊어진 경우 재연결을 시도합니다."""
        retry_interval = 5  # 초

        try:
            self.logger.info(f"MQTT 브로커 재연결 시도 중...")
            if self.mqtt_client:
                self.mqtt_client.connect(self.MQTT_HOST, self.MQTT_PORT)
            else:
                raise Exception("MQTT 클라이언트가 초기화되지 않았습니다.")
            return
        except Exception as e:
            self.logger.warning(f"MQTT 재연결 실패: {str(e)}. {retry_interval}초 후 재시도...")
            time.sleep(retry_interval)
        
    async def on_mqtt_connect(self, client: mqtt.Client, userdata: Any, flags: Dict[str, Any], rc: int) -> None:
        """MQTT 연결 성공/실패 시 호출되는 콜백"""
        if rc == 0:
            self.logger.info("MQTT 브로커 접속 완료")
            try:
                topics = [
                    (f'{self.HA_TOPIC}/+/+/command', 0),
                    (f'{self.ELFIN_TOPIC}/recv', 0),
                    (f'{self.ELFIN_TOPIC}/send', 0)
                ]
                client.subscribe(topics)
                # 온라인 상태 발행
                self.publish_mqtt(f"{self.HA_TOPIC}/status", "online", retain=True)
                self.is_available = True
            except Exception as e:
                self.logger.error(f"MQTT 토픽 구독 중 오류 발생: {str(e)}")
        else:
            errcode = {
                1: 'Connection refused - incorrect protocol version',
                2: 'Connection refused - invalid client identifier',
                3: 'Connection refused - server unavailable',
                4: 'Connection refused - bad username or password',
                5: 'Connection refused - not authorised'
            }
            error_msg = errcode.get(rc, '알 수 없는 오류')
            self.logger.error(f"MQTT 연결 실패: {error_msg}")

    def on_mqtt_message(self, client: mqtt.Client, userdata: Any, msg: mqtt.MQTTMessage) -> None:
        try:
            topics = msg.topic.split('/')
            
            if topics[0] == self.ELFIN_TOPIC:
                if topics[1] == 'recv':
                    self.elfin_reboot_count = 0            
                    if not self.is_available:
                        # 통신이 정상이고 offline 상태일 때 online으로 변경
                        self.publish_mqtt(f"{self.HA_TOPIC}/status", "online", retain=True)
                        self.is_available = True
                    raw_data = msg.payload.hex().upper()
                    self.logger.signal(f'->> 수신: {raw_data}')
                    if self.loop and self.loop.is_running():
                        asyncio.run_coroutine_threadsafe(
                            self.message_processor.process_elfin_data(raw_data),
                            self.loop
                        )
                    current_time = time.time_ns()
                    self.COLLECTDATA['last_recv_time'] = current_time
                    # 웹서버에 메시지 추가
                    self.web_server.add_mqtt_message(msg.topic, raw_data)
                    
                elif topics[1] == 'send':
                    raw_data = msg.payload.hex().upper()
                    self.logger.signal(f'<<- 송신: {raw_data}')
                    self.COLLECTDATA['send_data'].append(raw_data)
                    if len(self.COLLECTDATA['send_data']) > 300:
                        self.COLLECTDATA['send_data'] = list(self.COLLECTDATA['send_data'])[-300:]
                    # 웹서버에 메시지 추가
                    self.web_server.add_mqtt_message(msg.topic, raw_data)
                    
            elif topics[0] == self.HA_TOPIC:
                value = msg.payload.decode()
                self.logger.debug(f'->> 수신: {"/".join(topics)} -> {value}')
                # 웹서버에 메시지 추가
                self.web_server.add_mqtt_message("/".join(topics), value)
                
                if self.loop and self.loop.is_running():
                    asyncio.run_coroutine_threadsafe(
                        self.message_processor.process_ha_command(topics, value),
                        self.loop
                    )

        except Exception as err:
            self.logger.error(f'MQTT 메시지 처리 중 오류 발생: {str(err)}')

    def publish_mqtt(self, topic: str, value: Any, retain: bool = False) -> None:
        if self.mqtt_client:
            if topic.endswith('/send'):
                self.mqtt_client.publish(topic, value, retain=retain)
                self.logger.mqtt(f'{topic} >> {value}')
            else:
                self.mqtt_client.publish(topic, value.encode(), retain=retain)
                self.logger.mqtt(f'{topic} >> {value}')
        else:
            self.logger.error('MQTT 클라이언트가 초기화되지 않았습니다.')

    # 기기 검색 및 상태 관리 함수들
    @require_device_structure({})
    def find_device(self) -> Dict[str, Any]:
        """COLLECTDATA의 recv_data에서 기기를 찾는 함수입니다."""
        try:
            if not os.path.exists(self.share_dir):
                os.makedirs(self.share_dir)
                self.logger.info(f'{self.share_dir} 디렉토리를 생성했습니다.')
            
            save_path = os.path.join(self.share_dir, 'commax_found_device.json')
            
            assert isinstance(self.DEVICE_STRUCTURE, dict), "DEVICE_STRUCTURE must be a dictionary"
            
            state_headers = {
                self.DEVICE_STRUCTURE[name]["state"]["header"]: name 
                for name in self.DEVICE_STRUCTURE 
                if "state" in self.DEVICE_STRUCTURE[name]
            }
            self.logger.info(f'검색 대상 기기 headers: {state_headers}')
            
            device_count = {name: 0 for name in state_headers.values()}
            
            collect_data_set = set(self.COLLECTDATA['recv_data'])
            for data in collect_data_set:
                data_bytes = bytes.fromhex(data)
                header = byte_to_hex_str(data_bytes[0])
                if data == checksum(data) and header in state_headers:
                    name = state_headers[header]
                    self.logger.debug(f'감지된 기기: {data} {name} ')
                    try:
                        device_id_pos = self.DEVICE_STRUCTURE[name]["state"]["fieldPositions"]["deviceId"]
                        device_count[name] = max(
                            device_count[name],
                            #기기 갯수가 10개 넘어가면 HEX냐 DEC냐에 따라 갯수에 오류발생함!
                            int(byte_to_hex_str(data_bytes[int(device_id_pos)]),16)
                        )
                        self.logger.debug(f'기기 갯수 업데이트: {device_count[name]}')
                    except Exception as e:
                        #header가 존재하지만 deviceId가 없는 경우 1개로 처리
                        self.logger.debug(f'deviceId가 없는 기기: {name} {e}')
                        device_count[name] = 1
            
            # 검색 결과 처리
            self.logger.info('기기 검색 종료. 다음의 기기들을 찾았습니다...')
            self.logger.info('======================================')
            
            device_list = {}
            
            for name, count in device_count.items():
                assert isinstance(self.DEVICE_STRUCTURE, dict)  # 타입 체크 재확인
                device_list[name] = {
                    "type": self.DEVICE_STRUCTURE[name]["type"],
                    "count": count
                }
                self.logger.info(f'DEVICE: {name} COUNT: {count}')
            
            self.logger.info('======================================')
            
            # 검색 결과 저장
            try:
                with open(save_path, 'w', encoding='utf-8') as make_file:
                    json.dump(device_list, make_file, indent="\t")
                    self.logger.info(f'기기리스트 저장 완료: {save_path}')
            except IOError as e:
                self.logger.error(f'기기리스트 저장 실패: {str(e)}')
            
            return device_list
            
        except Exception as e:
            self.logger.error(f'기기 검색 중 오류 발생: {str(e)}')
            return {}

    async def reboot_elfin_device(self):
        try:
            if self.elfin_reboot_count > 10 and self.is_available:
                # availability 상태 업데이트
                self.publish_mqtt(f"{self.HA_TOPIC}/status", "offline", retain=True)
                self.is_available = False
            if self.elfin_unavailable_notification_enabled and self.elfin_reboot_count == 20: 
                # 20회 실패시 1회성 알림 전송 (기본값 60초 x 10 = 20분간 응답 없었음)
                self.logger.error('EW11 응답 없음')
                self.supervisor_api.send_notification(
                    title='[Commax Wallpad Addon] EW11 점검 및 재시작 필요',
                    message=f'[{time.strftime("%Y-%m-%d %H:%M:%S")}] EW11에서 응답이 없습니다. EW11 상태를 점검 후 애드온을 재시작 해주세요. 이 메시지를 확인했을 때 애드온이 다시 정상 작동 중이라면 무시해도 좋습니다.'
                    )
                return

            try:
                async with asyncio.timeout(10):  # 10초 타임아웃 설정
                    reader, writer = await telnetlib3.open_connection(
                        self.config['elfin'].get('elfin_server'),
                        connect_minwait=0.1,  # 최소 대기 시간
                        connect_maxwait=1.0   # 최대 대기 시간
                    )
                    assert reader and writer
                    try:
                        await reader.readuntil(b"login: ")
                        writer.write(self.config['elfin'].get('elfin_id') + '\n')
                        
                        await reader.readuntil(b"password: ")
                        writer.write(self.config['elfin'].get('elfin_password') + '\n')
                        
                        writer.write('Restart\n')
                        await writer.drain()  # 버퍼의 모든 데이터가 전송될 때까지 대기
                        
                    except Exception as e:
                        self.logger.error(f'텔넷 통신 중 오류 발생: {str(e)}')
                    finally:
                        writer.close()
                        
            except asyncio.TimeoutError:
                self.logger.error('텔넷 연결 시도 시간 초과')
            except Exception as e:
                self.logger.error(f'텔넷 연결 시도 중 오류 발생: {str(e)}')
            
            # 재시작 대기
            await asyncio.sleep(10)

        except Exception as err:
            self.logger.error(f'기기 재시작 프로세스 전체 오류: {str(err)}')

    async def process_queue(self) -> None:
        """큐에 있는 모든 명령을 처리하고 예상되는 응답을 확인합니다."""
        max_send_count = self.max_send_count
        if not self.QUEUE:
            return
        
        send_data = self.QUEUE.pop(0)
        
        try:
            cmd_bytes = bytes.fromhex(send_data['sendcmd'])
            self.publish_mqtt(f'{self.ELFIN_TOPIC}/send', cmd_bytes)
            send_data['count'] += 1
        except (ValueError, TypeError) as e:
            self.logger.error(f"명령 전송 중 오류 발생: {str(e)}")
            return
            
        expected_state = send_data.get('expected_state')
        if (isinstance(expected_state, dict)):
            
            required_bytes = expected_state['required_bytes']
            possible_values = expected_state['possible_values']
            
            recv_data_set = self.COLLECTDATA['recent_recv_data']
            for received_packet in recv_data_set:
                if not isinstance(received_packet, str):
                    continue

                try:
                    received_bytes = bytes.fromhex(received_packet)
                except ValueError:
                    continue
                match = True
                try:
                    for pos in required_bytes:
                        if not isinstance(pos, int):
                            self.logger.error(f"패킷 비교 중 오류 발생: {pos}는 정수가 아닙니다.")
                            match = False
                            break
                        if len(received_bytes) <= pos:
                            self.logger.error(f"패킷 비교 중 오류 발생: {pos}는 바이트 배열의 길이보다 큽니다.")
                            match = False
                            break
                            
                        if possible_values[pos]:
                            if byte_to_hex_str(received_bytes[pos]) not in possible_values[pos]:
                                match = False
                                break
                            
                except (IndexError, TypeError) as e:
                    self.logger.error(f"패킷 비교 중 오류 발생: {str(e)}")
                    match = False
                    
                if match:
                    send_data['received_count'] += 1
                    self.COLLECTDATA['recent_recv_data'] = set()
                    self.logger.debug(f"예상된 응답을 수신했습니다 ({send_data['received_count']}/{self.min_receive_count}): {received_packet}")
                    
            if send_data['received_count'] >= self.min_receive_count:
                return
            
            if send_data['count'] < max_send_count:
                self.logger.debug(f"명령 재전송 예약 (시도 {send_data['count']}/{max_send_count}): {send_data['sendcmd']}")
                self.QUEUE.insert(0, send_data)
            else:
                self.logger.warning(f"최대 전송 횟수 초과. 응답을 받지 못했습니다: {send_data['sendcmd']}")
                    
        # 예상 상태 정보가 없거나 잘못된 경우 재전송 시도만 함.
        else:
            if send_data['count'] < max_send_count:
                self.logger.debug(f"명령 전송 (횟수 {send_data['count']}/{max_send_count}): {send_data['sendcmd']}")
                self.QUEUE.insert(0, send_data)
        
        await asyncio.sleep(0.05)

    async def process_queue_and_monitor(self) -> None:
        """
        메시지 큐를 처리하고 기기 상태를 모니터링하는 함수입니다.

        1. 큐에 있는 메시지 처리 (130ms 이상 통신이 없을 때)
        2. ew11 기기 상태 모니터링 및 필요시 재시작

        Args:
            elfin_reboot_interval (int): ew11 기기 재시작 판단을 위한 통신 제한 시간 (초)

        Raises:
            Exception: 큐 처리 또는 기기 재시작 중 오류 발생시 예외를 발생시킵니다.
        """
        try:
            elfin_reboot_interval = self.config['elfin'].get('elfin_reboot_interval', 60)
            current_time = time.time_ns()
            last_recv = self.COLLECTDATA['last_recv_time']
            signal_interval = (current_time - last_recv)/1_000_000 #ns to ms
            
            if signal_interval > elfin_reboot_interval * 1_000:
                self.logger.warning(f'{elfin_reboot_interval}초간 신호를 받지 못했습니다.')
                self.COLLECTDATA['last_recv_time'] = time.time_ns()
                self.elfin_reboot_count += 1
                if (self.config['elfin'].get("use_auto_reboot",True)):
                    self.logger.warning(f'EW11 재시작을 시도합니다. {self.elfin_reboot_count}')
                    await self.reboot_elfin_device()
            if (self.send_command_on_idle):
                if signal_interval > 130: #130ms이상 여유있을 때 큐 실행
                    await self.process_queue()
            else:
                await self.process_queue()
            return
            
        except Exception as err:
            self.logger.error(f'process_queue_and_monitor() 오류: {str(err)}')
            return
        
    # 메인 실행 함수
    def run(self) -> None:
        self.logger.info("저장된 기기정보가 있는지 확인합니다. (/share/commax_found_device.json)")
        try:
            with open(self.share_dir + '/commax_found_device.json') as file:
                self.device_list = json.load(file)
            if not self.device_list:
                self.logger.info('기기 목록이 비어있습니다. 메인 루프 시작 후 기기 찾기를 시도합니다.')
            else:
                self.logger.info(f'기기정보를 찾았습니다. \n{json.dumps(self.device_list, ensure_ascii=False, indent=4)}')
        except IOError:
            self.logger.info('저장된 기기 정보가 없습니다. 메인 루프 시작 후 기기 찾기를 시도합니다.')
            self.device_list = {}

        try:
            # 웹서버 시작
            self.web_server.run()
            
            # 메인 MQTT 클라이언트 설정
            self.mqtt_client = self.setup_mqtt()
            
            # MQTT 연결 완료 이벤트를 위한 Event 객체 생성
            mqtt_connected = asyncio.Event()
            device_search_done = asyncio.Event()
            discovery_done = asyncio.Event()
            
            # 저장된 기기 정보가 있는 경우 device_search_done 설정
            if self.device_list:
                device_search_done.set()
                            
            # MQTT 콜백 설정
            def on_connect_callback(client: mqtt.Client, userdata: Any, flags: Dict[str, int], rc: int) -> None:
                if rc == 0:  # 연결 성공
                    if self.loop and self.loop.is_running():
                        asyncio.run_coroutine_threadsafe(
                            self.on_mqtt_connect(client, userdata, flags, rc), 
                            self.loop
                        )
                        mqtt_connected.set()
                    else:
                        self.logger.error("메인 루프가 실행되지 않았습니다.")
                        self.reconnect_mqtt()
                else:
                    # 재연결 시도
                    self.logger.error(f"MQTT 연결 실패 (코드: {rc})")

            def on_disconnect_callback(client: mqtt.Client, userdata: Any, rc: int) -> None:
                mqtt_connected.clear()  # 연결 해제 시 이벤트 초기화
                if rc != 0:
                    self.logger.error(f"예기치 않은 MQTT 연결 끊김 (코드: {rc})")
                    # 재연결 시도
                    self.reconnect_mqtt()
            
            self.mqtt_client.on_connect = on_connect_callback
            self.mqtt_client.on_disconnect = on_disconnect_callback
            self.mqtt_client.on_message = self.on_mqtt_message
            
            # MQTT 최초 연결
            self.connect_mqtt()
            
            self.mqtt_client.loop_start()
            
            # 메인 루프 실행
            self.loop = asyncio.new_event_loop()
            asyncio.set_event_loop(self.loop)

            # MQTT 연결 완료를 기다림
            async def wait_for_mqtt():
                no_recv_packet_count = 0
                queue_interval = float(self.config['command_settings'].get('queue_interval_in_second', 0.01))
                while True:
                    try:
                        await mqtt_connected.wait()
                        self.logger.info("MQTT 연결이 완료되었습니다. 메인 루프를 시작합니다.")
                        
                        while mqtt_connected.is_set():
                            if not discovery_done.is_set() and device_search_done.is_set():
                                await self.discovery_publisher.publish_discovery_message()
                                discovery_done.set()
                            # device_list가 비어있고 아직 기기 검색이 완료되지 않은 경우
                            recv_data_len = len(self.COLLECTDATA['recv_data'])
                            if not device_search_done.is_set():
                                if recv_data_len ==0:
                                    no_recv_packet_count += 1
                                    if no_recv_packet_count > 20:
                                        self.logger.warning("기기 검색 실패. EW11로부터 받은 패킷이 없습니다.")
                                        self.logger.warning("혹시 EW11 관리자 페이지에 MQTT설정이 되어있나요?")
                                        self.logger.warning("EW11 상태를 확인 후 애드온을 재시작 해주세요.")
                                        device_search_done.set()
                                self.logger.info(f"기기 검색을 위해 데이터 모으는중... {recv_data_len}/80")
                            if recv_data_len >= 80 and not device_search_done.is_set():
                                if not self.device_list:
                                    self.logger.info("충분한 데이터가 수집되어 기기 검색을 시작합니다.")
                                    self.device_list = self.find_device()
                                    if self.device_list:
                                        await self.discovery_publisher.publish_discovery_message()
                                        discovery_done.set()
                                    else:
                                        self.logger.warning("기기를 찾지 못했습니다.")
                                    device_search_done.set()
                            
                            await self.process_queue_and_monitor()
                            await asyncio.sleep(queue_interval)
                            
                    except Exception as e:
                        self.logger.error(f"메인 루프 실행 중 오류 발생: {str(e)}")
                        await asyncio.sleep(1)  # 오류 발생 시 1초 대기
                    
                    if not mqtt_connected.is_set():
                        self.logger.info("MQTT 재연결을 기다립니다...")
                        await asyncio.sleep(5)  # 재연결 시도 전 5초 대기
            
            # 메인 루프 실행
            self.loop.run_until_complete(wait_for_mqtt())
            
        except Exception as e:
            self.logger.error(f"실행 중 오류 발생: {str(e)}")
            raise
        finally:
            if self.loop:
                self.loop.close()
            if self.mqtt_client:
                self.mqtt_client.loop_stop()

    def __del__(self):
        """클래스 인스턴스가 삭제될 때 리소스 정리"""
        if hasattr(self, 'mqtt_client') and self.mqtt_client:
            self.mqtt_client.loop_stop()
            self.mqtt_client.disconnect()
        if self.loop and not self.loop.is_closed():
            self.loop.close()
            

if __name__ == '__main__':
    CONFIG_PATH = '/data/options.json'
    FALLBACK_PATH = './config.json'

    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH) as file:
            CONFIG = json.load(file)
    elif os.path.exists(FALLBACK_PATH):
        with open(FALLBACK_PATH) as file:
            CONFIG = json.load(file)["options"]
    else:
        CONFIG = {
            "mqtt": {
                "mqtt_server": os.getenv("MQTT_HOST", "localhost"),
                "mqtt_port": int(os.getenv("MQTT_PORT", "1883")),
                "mqtt_id": os.getenv("MQTT_USER", ""),
                "mqtt_password": os.getenv("MQTT_PASSWORD", "")
            },
            "elfin": {
                "elfin_unavailable_notification": False,
                "use_auto_reboot": True,
                "elfin_server": "192.168.50.200",  # 예시 IP
                "elfin_id": "admin",
                "elfin_password": "admin",
                "elfin_reboot_interval": 60
            },
            "log": {
                "DEBUG": True,
                "elfin_log": True,
                "mqtt_log": True
            },
            "command_settings": {
                "queue_interval_in_second": 0.1,
                "max_send_count": 20,
                "min_receive_count": 3,
                "send_command_on_idle": True
            }
        }

    logger = Logger(debug=CONFIG['log']['DEBUG'], elfin_log=CONFIG['log']['elfin_log'], mqtt_log=CONFIG['log']['mqtt_log'])
    logger.info("╔══════════════════════════════════════════╗")
    logger.info("║                                          ║")
    logger.info("║  Commax Wallpad Addon by ew11-mqtt 시작  ║") 
    logger.info("║                                          ║")
    logger.info("╚══════════════════════════════════════════╝")
    controller = WallpadController(CONFIG, logger)
    controller.run()