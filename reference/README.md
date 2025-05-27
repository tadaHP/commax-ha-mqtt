# Commax Wallpad Addon for Home Assistant

## 개발 관련 사항
python3 -m venv .venv -- 가상환경 만든 상황
source .venv/bin/activate -- 가상환경 실행


코맥스 월패드를 Home Assistant에 연동하기 위한 애드온입니다.
EW-11 전용입니다. usb-serial 통신은 아직 지원하지 않습니다.

이 애드온은 [@kimtc99](https://github.com/kimtc99/HAaddons)의 'CommaxWallpadBySaram' 애드온을 기반으로 작성되었으며 mqtt를 통해 ew11과 통신을 하는 특징이 있습니다.

- 명령 패킷 전송 시 예상되는 상태 패킷을 미리 계산하여 저장합니다.
- 예상된 상태 패킷이 수신될 때까지 명령 패킷을 자동으로 재전송합니다.
- 이를 통해 통신의 신뢰성을 높이고 명령이 제대로 처리되었는지 확인할 수 있습니다.
- 설정된 최대 재시도 횟수에 도달하면 재전송을 중단합니다.

다만 애드온을 거의 새로 작성하면서 저희집 월패드가 보일러만 있기 떄문에 보일러만 테스트 되었고 나머지 기능은 구현만 되어있습니다.

## 지원하는 기능
- 조명 제어 (디머 제어 x)
- 난방 제어
- 콘센트 제어
- 전열교환기 제어
- 가스밸브 잠금
- 엘리베이터 호출 (테스트 안되어있음)

## 설치 방법
1. Home Assistant의 Supervisor > Add-on Store에서 저장소 추가
2. 다음 URL을 저장소에 추가: `https://github.com/wooooooooooook/HAaddons`
3. 애드온 스토어에서 "Commax Wallpad Addon" 검색
4. "Install" 클릭 후 설치 진행

## EW11 mqtt 설정 방법 (필수!!)
EW11 관리페이지의 Community Settings에서 mqtt를 추가하고 다음과 같이 설정하세요:

### Basic Settings
- Name: mqtt
- Protocol: MQTT

### Socket Settings
- Server: 192.168.0.39 (MQTT 브로커의 IP 주소)
- Server Port: 1883
- Local Port: 0
- Buffer Size: 512
- Keep Alive(s): 60
- Timeout(s): 300

### Protocol Settings
- MQTT Version: 3
- MQTT Client ID: ew11-mqtt
- MQTT Account: my_user (mosquitto broker 애드온의 구성에서 확인하세요.)
- MQTT Password: m1o@s#quitto (mosquitto broker 애드온의 구성에서 확인하세요.)
- Subscribe Topic: ew11/send
- Subscribe QoS: 0
- Publish Topic: ew11/recv
- Publish QoS: 0
- Ping Period(s): 1

### More Settings
- Security: Disable
- Route: Uart


## 애드온 설정 방법
EW11 mqtt설정 후에 애드온 설정은 기본값으로 사용해도 무방합니다.

### 기본 설정
- `vendor`: 기기 패킷 구조 파일 선택 (commax/custom 선택), custom을 선택한경우 /share/packet_structures_custom.yaml을 우선적으로 적용하게됩니다.
- `mqtt_TOPIC`: MQTT 토픽 prefix (기본값: "commax")
- `elfin_TOPIC`: EW11 토픽 prefix (기본값: "ew11")

### 로그 설정
- `log.DEBUG`: 디버그 로그 출력 여부 (true/false)
- `log.mqtt_log`: MQTT 로그 출력 여부 (true/false)
- `log.elfin_log`: EW11 로그 출력 여부 (true/false)

### 명령 설정
- `command_settings.queue_interval_in_second`: 명령패킷 전송 간격 (초 단위, 기본값: 0.1 (100ms), 범위: 0.01-1.0)
- `command_settings.max_send_count`: 명령패킷 최대 재시도 횟수 (기본값: 15, 범위: 1-99)
- `command_settings.min_receive_count`: 패킷 전송 성공으로 판단할 예상패킷 최소 수신 횟수 (기본값: 1, 범위: 1-9)
- `command_settings.send_command_on_idle`: 월패드가 패킷전송을 잠시 쉴 때 (>130ms) 애드온에서 생성한 명령패킷을 전송하는 기능 (기본값 true)

### 온도조절기 설정
- `climate_settings.min_temp`: 온도조절기 최저 온도 제한 (기본값: 5°C, 범위: 0-19)
- `climate_settings.max_temp`: 온도조절기 최고 온도 제한 (기본값: 40°C, 범위: 20-99)

### MQTT 설정
기본 MQTT 브로커를 사용하려면 아래 설정들을 비워두세요:
- `mqtt.mqtt_server`: MQTT 브로커 서버 주소
- `mqtt.mqtt_port`: MQTT 브로커 포트 (기본값: 1883)
- `mqtt.mqtt_id`: MQTT 사용자 아이디
- `mqtt.mqtt_password`: MQTT 비밀번호

### EW11 (Elfin) 설정
- `elfin.use_auto_reboot`: EW11 자동 재부팅 사용 여부 (true/false)
- `elfin.elfin_unavailable_notification`: EW11 응답 없을 때 HA 알림 생성 여부 (true/false) 
- `elfin.elfin_server`: EW11 장치의 IP 주소 (재부팅기능에 사용)
- `elfin.elfin_id`: EW11 관리자 아이디 (재부팅기능에 사용)
- `elfin.elfin_password`: EW11 관리자 비밀번호 (재부팅기능에 사용)
- `elfin.elfin_reboot_interval`: EW11 자동 재부팅 간격 (초 단위, 기본값: 60)

설정 예시:
```yaml
vendor: "commax"
mqtt_TOPIC: "commax"
elfin_TOPIC: "ew11"

log:
  DEBUG: false
  mqtt_log: false
  elfin_log: false

command_settings:
  queue_interval_in_second: 0.1
  max_send_count: 15
  min_receive_count: 1
  send_command_on_idle: true

climate_settings:
  min_temp: 5
  max_temp: 40

mqtt:
  mqtt_server: ""  # 기본 브로커 사용시 비워두세요
  mqtt_port: 1883
  mqtt_id: ""
  mqtt_password: ""

elfin:
  use_auto_reboot: true
  elfin_unavailable_notification: false
  elfin_server: "192.168.0.38"
  elfin_id: "admin"
  elfin_password: "admin"
  elfin_reboot_interval: 60
```

## 커스텀 패킷구조 지원

패킷 값이 다른 경우 웹UI의 '커스텀 패킷 구조 편집' 메뉴에서 수정하여 사용할 수 있습니다.
웹UI - '플레이그라운드'에서 올라오고있는 패킷구조를 확인하여 커스텀 패킷구조를 작성할 수 있습니다.

## 엘리베이터를 활성화 하는방법

애드온에서 기기검색 중에 엘리베이터 상태 패킷이 올라오면됩니다.
1. 웹UI에서 기기검색을 누른다(기기목록 초기화 후 재시작하여 기기검색을 다시 수행)
2. 월패드에서 엘베 호출을 누른다
3. 엘베 상태 패킷이 올라오며 애드온에서 엘베 버튼을 추가함.
또는 ```/share/commax_found_devices.json```파일을 직접 수정하여 EV의 count를 1로 수정후 애드온을 재시작하면 엘베호출버튼이 생성됩니다

## 대기전력차단 콘센트 scailing_factor
기본적으로 패킷의 5~7번째 바이트를 연속으로 읽어서 전력량으로 반환합니다.
소비전력량의 경우 wattage_scailing_factor(기본값 0.1)를, 대기전력차단값의경우 ecomode_scailing_factor(기본값 1)를 곱해서 표시합니다.
scailig_factor 값의 변경이 필요한경우 `웹UI - 패킷 구조 편집`에서 custom활성화가 안된경우 활성화 시켜주시고 Outlet tab에서 값을 수정하시면됩니다.

## 기타
- elfin_reboot_interval값 x 10 동안 ew11 응닶없음 -> 구성요소들이 사용불가 (unavailable)상태로 변경됩니다.
- elfin_reboot_interval값 x 20 동안 ew11 응닶없음 -> elfin_unavailable_notification 값이 true일 경우 HA 알림이 발생합니다.
