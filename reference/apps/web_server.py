from flask import Flask, render_template, jsonify, request # type: ignore
import threading
import logging
import asyncio
import os
from typing import Dict, Any
import time
import json
import yaml # type: ignore
import shutil
from datetime import datetime
import requests # type: ignore
from .utils import checksum
from gevent.pywsgi import WSGIServer # type: ignore
import sys
from .supervisor_api import SupervisorAPI

class WebServer:
    def __init__(self, wallpad_controller):
        # Flask 로깅 완전 비활성화
        logging.getLogger('werkzeug').disabled = True
        cli = sys.modules['flask.cli']
        cli.show_server_banner = lambda *x: None # type: ignore
        
        self.app = Flask(__name__, template_folder='templates', static_folder='static')
        self.app.config['SEND_FILE_MAX_AGE_DEFAULT'] = 0  # static 파일 캐싱 비활성화
        self.app.logger.disabled = True
        self.wallpad_controller = wallpad_controller
        self.logger = wallpad_controller.logger
        self.supervisor_api = SupervisorAPI()
        
        # addon_info 초기화
        addon_info_result = self.supervisor_api.get_addon_info()
        self.addon_info = addon_info_result.data if addon_info_result.success else None
        
        self.recent_messages = {}
        self.server = None
        
        @self.app.after_request
        def add_header(response):
            response.headers['Cache-Control'] = 'no-store, no-cache, must-revalidate, post-check=0, pre-check=0, max-age=0'
            response.headers['Pragma'] = 'no-cache'
            response.headers['Expires'] = '-1'
            return response
        @self.app.errorhandler(Exception)
        def handle_exception(e):
            return jsonify({"success": False, "error": str(e)}), 500
        
        # 라우트 설정
        @self.app.route('/')
        def home():
            return render_template('index.html')

        @self.app.route('/api/live_packets')
        def live_packets():
            """실시간 패킷 데이터를 반환하는 API"""
            return jsonify({
                'send_data': self.wallpad_controller.COLLECTDATA['send_data'],
                'recv_data': self.wallpad_controller.COLLECTDATA['recv_data']
            })
        @self.app.route('/api/custom_packet_structure/editable', methods=['GET'])
        def get_editable_packet_structure():
            """편집 가능한 패킷 구조 필드를 반환합니다."""
            try:
                custom_file = '/share/packet_structures_custom.yaml'
                if os.path.exists(custom_file):
                    with open(custom_file, 'r', encoding='utf-8') as f:
                        data = yaml.safe_load(f)
                else:
                    with open('/apps/packet_structures_commax.yaml', 'r', encoding='utf-8') as f:
                        data = yaml.safe_load(f)

                editable_structure = {}
                for device_name, device_data in data.items():
                    editable_structure[device_name] = {
                        'type': device_data.get('type', ''),
                        'command': self._get_editable_fields(device_data.get('command', {})),
                        'state': self._get_editable_fields(device_data.get('state', {})),
                        'state_request': self._get_editable_fields(device_data.get('state_request', {})),
                        'ack': self._get_editable_fields(device_data.get('ack', {}))
                    }
                return jsonify({'content': editable_structure, 'success': True})
            except Exception as e:
                return jsonify({'error': str(e), 'success': False})

        @self.app.route('/api/custom_packet_structure/editable', methods=['POST'])
        def save_editable_packet_structure():
            """편집된 패킷 구조를 기존 구조와 병합하여 저장합니다."""
            try:
                if not request.json:
                    return jsonify({'error': '요청 데이터가 없습니다.', 'success': False})
                content = request.json.get('content', {})
                
                # 현재 패킷 구조 로드
                custom_file = '/share/packet_structures_custom.yaml'
                if os.path.exists(custom_file):
                    with open(custom_file, 'r', encoding='utf-8') as f:
                        current_data = yaml.safe_load(f)
                else:
                    with open('/apps/packet_structures_commax.yaml', 'r', encoding='utf-8') as f:
                        current_data = yaml.safe_load(f)

                # 편집된 내용을 현재 구조와 병합
                for device_name, device_data in content.items():
                    if device_name not in current_data:
                        current_data[device_name] = {}
                    
                    current_data[device_name]['type'] = device_data.get('type', current_data[device_name].get('type', ''))
                    
                    for packet_type in ['command', 'state', 'state_request', 'ack']:
                        if packet_type in device_data:
                            self._merge_packet_structure(
                                current_data[device_name].setdefault(packet_type, {}),
                                device_data[packet_type]
                            )

                # 백업 생성
                backup_dir = '/share/packet_structure_backups'
                if not os.path.exists(backup_dir):
                    os.makedirs(backup_dir)
                
                if os.path.exists(custom_file):
                    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
                    backup_file = f'{backup_dir}/packet_structures_custom_{timestamp}.yaml'
                    shutil.copy2(custom_file, backup_file)

                # 새 내용 저장
                with open(custom_file, 'w', encoding='utf-8') as f:
                    yaml.dump(current_data, f, allow_unicode=True, sort_keys=False)

                # 컨트롤러의 패킷 구조 다시 로드
                self.wallpad_controller.load_devices_and_packets_structures()

                return jsonify({'success': True})
            except Exception as e:
                return jsonify({'error': str(e), 'success': False})

        @self.app.route('/api/devices')
        def get_devices():
            return jsonify(self.wallpad_controller.device_list or {})
            
        @self.app.route('/api/mqtt_status')
        def get_mqtt_status():
            """MQTT 연결 상태 정보를 제공합니다."""
            if not self.wallpad_controller.mqtt_client:
                return jsonify({
                    'connected': False,
                    'broker': None,
                    'client_id': None,
                    'subscribed_topics': []
                })

            client = self.wallpad_controller.mqtt_client
            return jsonify({
                'connected': client.is_connected(),
                'broker': f"{self.wallpad_controller.MQTT_HOST}",
                'client_id': client._client_id.decode() if client._client_id else None,
                'subscribed_topics': [
                    f'{self.wallpad_controller.HA_TOPIC}/+/+/command',
                    f'{self.wallpad_controller.ELFIN_TOPIC}/recv',
                    f'{self.wallpad_controller.ELFIN_TOPIC}/send'
                ]
            })

        @self.app.route('/api/config', methods=['GET'])
        def get_config():
            """CONFIG 객체의 내용과 스키마를 제공합니다."""
            # addon_info에서 schema 정보 가져오기
            schema = self.addon_info.get('schema', {}) if self.addon_info else {}
            
            return jsonify({
                'config': self.wallpad_controller.config,
                'schema': schema
            })
        
        @self.app.route('/api/config', methods=['POST'])
        def save_config():
            """설정을 저장하고 컨트롤러에 적용합니다."""
            try:
                data = request.json
                if not data:
                    return jsonify({'error': '설정 데이터가 없습니다.'}), 400

                # addon_info에서 현재 설정 가져오기
                current_options = self.addon_info.get('options', {}) if self.addon_info else {}
                
                try:
                    updated_options = current_options.copy()
                    updated_options.update(data)

                    # SupervisorAPI를 사용하여 설정 업데이트
                    update_result = self.supervisor_api.update_addon_options(updated_options)
                    if not update_result.success:
                        return jsonify({
                            'success': False,
                            'error': update_result.message
                        }), 500

                    # SupervisorAPI를 사용하여 애드온 재시작
                    restart_result = self.supervisor_api.restart_addon()
                    if not restart_result.success:
                        return jsonify({
                            'success': False,
                            'error': restart_result.message
                        }), 500
                    
                    return jsonify({
                        'success': True,
                        'message': '설정을 저장했습니다. 이 메시지는 애드온이 재시작되어 전달되지 못합니다.'
                    })

                except Exception as e:
                    return jsonify({'error': str(e)}), 500
            except Exception as e:
                return jsonify({'error': str(e)}), 500

        @self.app.route('/api/recent_messages')
        def get_recent_messages():
            """최근 MQTT 메시지 목록을 제공합니다."""
            return jsonify({
                'messages': self.recent_messages  # 전체 딕셔너리 반환
            })

        @self.app.route('/api/packet_logs')
        def get_packet_logs():
            """패킷 로그를 제공합니다.
            
            Returns:
                dict: {
                    'send': list[dict] - 송신 패킷 목록
                        - packet: str - 패킷 데이터
                        - results: dict - 패킷 분석 결과
                            - device: str - 기기 종류
                            - packet_type: str - 패킷 타입 ['command', 'state_request', 'state', 'ack']
                    'recv': list[dict] - 수신 패킷 목록 (송신 패킷과 동일한 구조)
                }
            """
            try:
                send_packets = []
                recv_packets = []

                # 송신/수신 패킷 처리
                for data_set, packets_list in [
                    (set(self.wallpad_controller.COLLECTDATA['send_data']), send_packets),
                    (set(self.wallpad_controller.COLLECTDATA['recv_data']), recv_packets)
                ]:
                    for packet in data_set:
                        packet_info = {
                            'packet': packet,
                            'results': {
                                'device': 'Unknown',
                                'packet_type': 'Unknown'
                            }
                        }
                        device_info = self._analyze_packet_structure(packet)
                        if device_info['success']:
                            packet_info['results'] = {
                                'device': device_info['device'],
                                'packet_type': device_info['packet_type']
                            }
                        packets_list.append(packet_info)

                return jsonify({
                    'send': send_packets,
                    'recv': recv_packets
                })

            except Exception as e:
                return jsonify({
                    'error': str(e)
                }), 500

        @self.app.route('/api/find_devices', methods=['POST'])
        def find_devices():
            try:
                # 기존 기기 목록 파일 삭제
                if os.path.exists('/share/commax_found_device.json'):
                    os.remove('/share/commax_found_device.json')
                
                # SupervisorAPI를 사용하여 애드온 재시작
                restart_result = self.supervisor_api.restart_addon()
                if not restart_result.success:
                    return jsonify({
                        'success': False,
                        'error': restart_result.message
                    }), 500
                
                return jsonify({
                    'success': True,
                    'message': '기기 검색을 시작합니다. 이 메시지는 애드온이 재시작되어 전달되지 못합니다.'
                })

            except Exception as e:
                return jsonify({
                    'success': False,
                    'error': str(e)
                }), 500

        @self.app.route('/api/analyze_packet', methods=['POST'])
        def analyze_packet():
            try:
                data = request.get_json()
                command = data.get('command', '').strip()

                # 체크섬 계산
                checksum_result = checksum(command)

                # 패킷 구조 분석
                analysis_result = self._analyze_packet_structure(command)

                if not analysis_result["success"]:
                    return jsonify(analysis_result), 400

                response = {
                    "success": True,
                    "device": analysis_result["device"],
                    "analysis": analysis_result["analysis"],
                    "checksum": checksum_result
                }

                # command 패킷인 경우 예상 상태 패킷 추가
                if analysis_result.get("packet_type") == "command" and checksum_result:
                    expected_state = self.wallpad_controller.message_processor.generate_expected_state_packet(checksum_result)
                    if expected_state:
                        response["expected_state"] = {
                            "required_bytes": expected_state["required_bytes"],
                            "possible_values": expected_state["possible_values"]
                        }

                return jsonify(response)

            except Exception as e:
                return jsonify({
                    "success": False,
                    "error": str(e)
                }), 400

        @self.app.route('/api/packet_structures')
        def get_packet_structures():
            structures = {}
            for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
                structures[device_name] = {
                    "type": device['type'],
                    "command": self._get_packet_structure(device_name, device, 'command'),
                    "state": self._get_packet_structure(device_name, device, 'state'),
                    "state_request": self._get_packet_structure(device_name, device, 'state_request'),
                    "ack": self._get_packet_structure(device_name, device, 'ack')
                }
             
            return jsonify(structures)

        @self.app.route('/api/packet_suggestions')
        def get_packet_suggestions():
            """패킷 입력 도우미를 위한 정보를 제공합니다."""
            suggestions = {
                'headers': {},  # 헤더 정보
                'values': {}    # 각 바이트 위치별 가능한 값
            }
            
            # 명령 패킷 헤더
            command_headers = []
            for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
                if 'command' in device:
                    command_headers.append({
                        'header': device['command']['header'],
                        'device': device_name
                    })
            suggestions['headers']['command'] = command_headers
            
            # 상태 패킷 헤더
            state_headers = []
            for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
                if 'state' in device:
                    state_headers.append({
                        'header': device['state']['header'],
                        'device': device_name
                    })
            suggestions['headers']['state'] = state_headers
            
            # 상태 요청 패킷 헤더
            state_request_headers = []
            for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
                if 'state_request' in device:
                    state_request_headers.append({
                        'header': device['state_request']['header'],
                        'device': device_name
                    })
            suggestions['headers']['state_request'] = state_request_headers
            
            # 응답 패킷 헤더
            ack_headers = []
            for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
                if 'ack' in device:
                    ack_headers.append({
                        'header': device['ack']['header'],
                        'device': device_name
                    })
            suggestions['headers']['ack'] = ack_headers
            
            # 각 기기별 가능한 값들
            for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
                for packet_type in ['command', 'state', 'state_request', 'ack']:
                    if packet_type in device:
                        key = f"{device_name}_{packet_type}"
                        suggestions['values'][key] = {}
                        
                        for pos, field in device[packet_type]['structure'].items():
                            if 'values' in field:
                                suggestions['values'][key][pos] = {
                                    'name': field['name'],
                                    'values': field['values']
                                }
            
            return jsonify(suggestions)

        @self.app.route('/api/send_packet', methods=['POST'])
        def send_packet():
            try:
                data = request.get_json()
                packet = data.get('packet', '').strip()
                
                if not packet:
                    return jsonify({"success": False, "error": "패킷이 비어있습니다."}), 400
                
                if packet != checksum(packet):
                    return jsonify({"success": False, "error": "잘못된 패킷입니다."}), 400
                
                loop = asyncio.get_event_loop()
                loop.create_task(self.wallpad_controller.message_processor.process_elfin_data(packet))
                
                packet_bytes = bytes.fromhex(packet)
                self.wallpad_controller.publish_mqtt(f'{self.wallpad_controller.ELFIN_TOPIC}/send', packet_bytes)
                
                return jsonify({"success": True})
            
            except Exception as e:
                self.logger.error(f"웹UI 패킷 전송 실패: {str(e)}")
                return jsonify({"success": False, "error": str(e)}), 500


        @self.app.route('/api/custom_packet_structure', methods=['GET'])
        def get_custom_packet_structure():
            """커스텀 패킷 구조 파일의 내용을 반환합니다."""
            try:
                custom_file = '/share/packet_structures_custom.yaml'
                if os.path.exists(custom_file):
                    with open(custom_file, 'r', encoding='utf-8') as f:
                        content = f.read()
                else:
                    # 기본 패킷 구조 파일 읽기
                    with open('/apps/packet_structures_commax.yaml', 'r', encoding='utf-8') as f:
                        content = f.read()
                return jsonify({'content': content, 'success': True})
            except Exception as e:
                return jsonify({'error': str(e), 'success': False})

        @self.app.route('/api/custom_packet_structure', methods=['DELETE'])
        def delete_custom_packet_structure():
            """커스텀 패킷 구조 파일을 삭제하고 기본값으로 초기화합니다."""
            try:
                custom_file = '/share/packet_structures_custom.yaml'
                if os.path.exists(custom_file):
                    os.remove(custom_file)
                return jsonify({'success': True})
            except Exception as e:
                return jsonify({'error': str(e), 'success': False})

        @self.app.route('/api/custom_packet_structure', methods=['POST'])
        def save_custom_packet_structure():
            """커스텀 패킷 구조 파일을 저장합니다."""
            try:
                if not request.json:
                    return jsonify({'error': 'JSON 형식이 아닙니다.', 'success': False}), 400
                    
                content = request.json.get('content', '')
                if not content:
                    return jsonify({'error': '내용이 비어있습니다.', 'success': False}), 400
                
                # YAML 유효성 검사
                try:
                    yaml.safe_load(content)
                except yaml.YAMLError as e:
                    return jsonify({'error': f'YAML 형식이 잘못되었습니다: {str(e)}', 'success': False})

                # 백업 생성
                backup_dir = '/share/packet_structure_backups'
                if not os.path.exists(backup_dir):
                    os.makedirs(backup_dir)
                
                custom_file = '/share/packet_structures_custom.yaml'
                if os.path.exists(custom_file):
                    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
                    backup_file = f'{backup_dir}/packet_structures_custom_{timestamp}.yaml'
                    shutil.copy2(custom_file, backup_file)

                # 새 내용 저장
                with open(custom_file, 'w', encoding='utf-8') as f:
                    f.write(content)

                return jsonify({'success': True})
            except Exception as e:
                return jsonify({'error': str(e), 'success': False})

        @self.app.route('/api/ew11_status')
        def get_ew11_status():
            try:
                last_recv_time = self.wallpad_controller.COLLECTDATA.get('last_recv_time', 0)
                elfin_reboot_interval = self.wallpad_controller.config['elfin'].get('elfin_reboot_interval', 10)
                
                return jsonify({
                    'last_recv_time': last_recv_time,
                    'elfin_reboot_interval': elfin_reboot_interval
                })
            except Exception as e:
                self.logger.error(f"웹UI EW11 상태 조회 실패: {str(e)}")
                return jsonify({'error': str(e)}), 500

    def _get_editable_fields(self, packet_data):
        """패킷 구조에서 편집 가능한 필드만 추출합니다."""
        if not packet_data:
            return {}
            
        result = {
            'header': packet_data.get('header', ''),
            'structure': {}
        }
        
        if 'structure' in packet_data:
            for position, field in packet_data['structure'].items():
                result['structure'][position] = {
                    'name': field.get('name', ''),
                    'values': field.get('values', {})
                }
        
        # 추가 설정 항목 처리
        for key, value in packet_data.items():
            if key not in ['header', 'structure']:
                result[key] = value
                
        return result

    def _merge_packet_structure(self, current, new):
        """현재 패킷 구조와 새로운 패킷 구조를 병합합니다."""
        if 'header' in new:
            current['header'] = new['header']
            
        if 'structure' in new:
            if 'structure' not in current:
                current['structure'] = {}
                
            for position, field in new['structure'].items():
                if position not in current['structure']:
                    current['structure'][position] = {}
                    
                current['structure'][position]['name'] = field.get('name', '')
                current['structure'][position]['values'] = field.get('values', {})
        
        # 추가 설정 항목 병합
        for key, value in new.items():
            if key not in ['header', 'structure']:
                current[key] = value

    def _analyze_packet_structure(self, command: str) -> Dict[str, Any]:
        """패킷 구조를 분석하고 관련 정보를 반환합니다."""
        # 헤더 기기 찾기
        header = command[:2]
        device_info = None
        device_name = None
        packet_type = None

        for name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
            for ptype in ['command', 'state', 'state_request', 'ack']:
                if ptype in device and device[ptype]['header'] == header:
                    device_info = device[ptype]
                    device_name = name
                    packet_type = ptype
                    break
            if device_info:
                break

        if not device_info:
            return {
                "success": False,
                "error": f"알 수 없는 패킷입니다."
            }

        # 각 바이트 분석
        byte_analysis = []
        # 헤더 추가
        byte_analysis.append(f"Byte 0: header = {device_name} {packet_type} ({header})")

        for pos, field in device_info['structure'].items():
            pos = int(pos)
            if pos * 2 + 2 <= len(command):
                byte_value = command[pos*2:pos*2+2]
                desc = f"Byte {pos}: {field['name']}"

                if field['name'] == 'empty':
                    desc = f"Byte {pos}: (00)"
                elif field['name'] == 'checksum':
                    desc = f"Byte {pos}: 체크섬"
                elif 'values' in field:
                    # 알려진 값과 매칭
                    matched_value = None
                    for key, value in field['values'].items():
                        if value == byte_value:
                            matched_value = key
                            break
                    if matched_value:
                        desc += f" = {matched_value} ({byte_value})"
                    else:
                        desc += f" = {byte_value}"
                else:
                    desc += f" = {byte_value}"

                byte_analysis.append(desc)

        return {
            "success": True,
            "device": device_name,
            "packet_type": packet_type,
            "analysis": byte_analysis
        }

    def _get_packet_structure(self, device_name: str, device: Dict[str, Any], packet_type: str) -> Dict[str, Any]:
        """패킷 구조 정보를 생성합니다."""
        if packet_type not in device:
            return {}

        structure = device[packet_type]
        byte_desc = {}
        byte_values = {}
        byte_memos = {}  # memo 정보를 저장할 딕셔너리
        examples = []

        # 헤더 설명
        byte_desc[0] = f"header ({structure['header']})"
        
        # 각 바이트 설명과 값 생성
        for pos, field in structure['structure'].items():
            pos = int(pos)
            if field['name'] == 'empty':
                byte_desc[pos] = "00"
            else:
                byte_desc[pos] = field['name']
                if 'values' in field:
                    byte_values[pos] = field['values']
                if 'memo' in field:  # memo 필드가 있는 경우 저장
                    byte_memos[pos] = field['memo']
        
        return {
            "header": structure['header'],
            "byte_desc": byte_desc,
            "byte_values": byte_values,
            "byte_memos": byte_memos,  # memo 정보 추가
        }

    def _get_device_info(self, packet: str) -> Dict[str, str]:
        """패킷의 헤더를 기반으로 기기 정보를 반환합니다."""
        if len(packet) < 2:
            return {"name": "Unknown", "packet_type": "Unknown"}
            
        header = packet[:2]
        
        # 명령 패킷 확인
        for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
            if 'command' in device and device['command']['header'] == header:
                return {"name": device_name, "packet_type": "Command"}
                
        # 상태 패킷 확인
        for device_name, device in self.wallpad_controller.DEVICE_STRUCTURE.items():
            if 'state' in device and device['state']['header'] == header:
                return {"name": device_name, "packet_type": "State"}
                
        return {"name": "Unknown", "packet_type": "Unknown"}

    def add_mqtt_message(self, topic: str, payload: str) -> None:
        """MQTT 메시지를 토픽별로 저장합니다. 각 토픽당 최신 메시지만 유지합니다."""
        self.recent_messages[topic] = {
            'payload': payload,
            'timestamp': time.strftime('%Y-%m-%d %H:%M:%S')
        }

    def run(self):
        # Flask 서버 실행
        self.server = WSGIServer(('0.0.0.0', 8099), self.app, log=None)
        # 별도의 스레드에서 서버 실행
        threading.Thread(target=self._run_server, daemon=True).start()
        
    def _run_server(self):
        try:
            self.logger.info("웹서버 시작")
            assert self.server is not None
            self.server.serve_forever()
        except Exception as e:
            self.logger.error(f"Server error: {e}")
            
    def stop(self):
        if self.server:
            self.logger.info("웹서버 종료")
            self.server.stop()
            self.server = None 