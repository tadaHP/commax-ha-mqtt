# Wallpad Controller

## image build

```bash
./gradlew build
docker build -t wallpad-controller .
docker tag wallpad-controller:latest ghcr.io/tadahp/commax-wallpad:latest
docker push ghcr.io/tadahp/commax-wallpad:latest
```

---

# Commax Wallpad - MQTT With Docker

이 프로젝트는 [@kimtc99](https://github.com/kimtc99/HAaddons)의 'CommaxWallpadBySaram' 애드온을 기반으로 작성된
[@wooooooooooook](https://github.com/wooooooooooook/HAaddons#)님의 애드온을 기반으로 작성되었으며
mqtt 또는 udp를 통해 elfin-ew11과 통신을 하여, HA에서 MQTT 통합 구성요소로 통합 가능하게 만든 특징이 있습니다.

기본 베이스 코드는 위 코드를 기반으로 진행하며, Spring을 통해 구동하도록 개발 예정입니다.

Docker로 HA를 이용하는 사용자들을 위한 프로젝트로
기존 MQTT 통합구성요소를 통해 Commax 월패드를 통합 예정입니다.

LICENSE는 AGPL 3.0 을 따라 자유로운 수전 및 재배포가 가능하나 소스코드 공개가 필수이며 상업적 사용도 가능하나 오픈소스로 유지하여야합니다.

# 사용법

**DB (H2):** 예전에 `SPRING_DATASOURCE_URL=jdbc:sqlite:...` 를 쓰던 compose는 **반드시 제거하거나** `jdbc:h2:file:...` 로 바꾸세요. SQLite URL이 남아 있으면 `org.h2.Driver`가 URL을 거부해 기동에 실패합니다. URL을 안 주면 docker 프로필 기본값(`jdbc:h2:file:/app/commax`)이 적용됩니다.

사용 예제는 DockerCompose 기준으로 작성합니다.

2가지 모드가있으며 1. MQTT(기존모드) 와 2. UDP 모드가 있습니다.

위 모드에 따라 ew11 과의 통신방법을 결정합니다 (Home Assistant와 통신 방법은 MQTT로 고정).
UDP 모드는 ew11을 UDP로 설정하고 컨테이너가 UDP로 수신/송신하도록 구성합니다.

## mqtt모드

```yml
services:
  wallpadcontroller:
    image: ghcr.io/tadahp/commax-wallpad:latest
    container_name: wallpadcontroller
    restart: unless-stopped
    volumes:
      - commax-wallpad:/app
    ports:
      - "52394:8080"
    environment:
      - MQTT_HOST=192.168.0.0
      - MQTT_PORT=1883
      - MQTT_CLIENT_ID=wallpad-controller
      - MQTT_USERNAME=
      - MQTT_PASSWORD=
      - MQTT_HA_TOPIC=commax
      - EW11_TRANSPORT=mqtt
      - EW11_MQTT_SEND_TOPIC=ew11/send
      - EW11_MQTT_RECEIVE_TOPIC=ew11/recv
volumes:
  commax-wallpad:
```

## udp모드

EW11을 UDP 모드로 설정한 뒤, 아래 환경변수로 송신 대상과 수신 포트를 맞춰주세요.

```yaml
services:
  wallpadcontroller:
    image: ghcr.io/tadahp/commax-wallpad:latest
    container_name: wallpadcontroller
    restart: unless-stopped
    volumes:
      - commax-wallpad:/app
    ports:
      - "52394:8080"
      - "54747:54747/udp"
    environment:
      - MQTT_HOST=192.168.0.0
      - MQTT_PORT=1883
      - MQTT_CLIENT_ID=wallpad-controller
      - MQTT_USERNAME=
      - MQTT_PASSWORD=
      - MQTT_HA_TOPIC=commax
      - EW11_TRANSPORT=udp
      - EW11_UDP_SEND_HOST=192.168.0.20
      - EW11_UDP_SEND_PORT=52493
      - EW11_UDP_LISTEN_PORT=54747
      - EW11_UDP_BUFFER_SIZE=512
volumes:
  commax-wallpad:
```

위 내용중 환경변수에 맞게 채워넣으시면 작동하며, 빈값은 옵션입니다.

## Home Assistant 재기동 후 상태 복구

CWC는 Home Assistant MQTT 통합의 birth message를 구독합니다. HA가 MQTT에 `online`을 발행하면 5초 뒤 CWC 메모리에 있는 조명, 보일러 등 현재 상태 전체를 다시 발행합니다. 따라서 HA만 재기동한 경우에도 다음 상태 패킷이나 외부 조작을 기다리지 않고 상태를 복구할 수 있습니다.

기본값은 `homeassistant/status` topic의 `online` payload입니다. **CWC 내부 YAML 파일을 수정할 필요 없이**, Docker Compose의 `environment`에 아래 값만 넣거나 변경하면 됩니다. HA에서 birth message topic/payload를 별도로 설정했다면 그 값과 동일하게 맞추세요.

| 환경변수 | 설명 | 기본값 |
| --- | --- | --- |
| `MQTT_HA_BIRTH_TOPIC` | HA MQTT birth message topic | `homeassistant/status` |
| `MQTT_HA_BIRTH_PAYLOAD` | HA가 기동 완료 시 발행하는 payload | `online` |

```yaml
services:
  wallpadcontroller:
    environment:
      # HA MQTT integration 기본 birth message를 쓸 경우 생략 가능
      - MQTT_HA_BIRTH_TOPIC=homeassistant/status
      - MQTT_HA_BIRTH_PAYLOAD=online
```

## 난방 주기적 상태조회

활성화된 난방기(`Thermo`, `used=true` 또는 기존 데이터의 `null`)에 상태조회 패킷을 자동 전송합니다. 기본적으로 시작 5초 후 조회를 시작하고, 한 차례 조회가 끝나면 5초 후 다시 조회합니다. 기기 사이에는 100ms 간격을 둡니다. 새로 발견되어 비활성 상태인 기기는 조회하지 않습니다.

패킷은 `02 <기기번호> 00 00 00 00 00 <합산 체크섬>`이며, 예를 들어 1번 난방기는 `02 01 00 00 00 00 00 03`입니다. 응답 상태는 기존 수신·파싱·MQTT 상태 발행 경로에서 처리합니다. 조회 자체는 제어 명령의 ACK 재시도 대상이 아닙니다.

| 환경변수 | 설명 | 기본값 |
| --- | --- | --- |
| `HEATING_POLLING_ENABLED` | 난방 주기 조회 사용 여부 | `true` |
| `HEATING_POLLING_INTERVAL` | 전체 조회 완료 후 다음 조회까지 간격 | `5s` |
| `HEATING_POLLING_DEVICE_GAP` | 난방기 간 전송 간격 | `100ms` |

주기와 기기 간 간격은 양수여야 합니다. 월패드가 이미 충분히 자주 상태를 조회한다면 `HEATING_POLLING_ENABLED=false`로 끌 수 있습니다. 조명 주기 조회는 포함하지 않습니다.

## EW11 주기 재부팅 (옵션)

**UDP / MQTT 모드 공통.** EW11 웹 API(`http://<호스트>/cmd`)에 CID 20003(재부팅) 요청을 주기적으로 보냅니다. 호스트는 항상 `EW11_REBOOT_HOST`로 지정합니다.

| 환경변수               | 설명                                 | 기본값  |
| ---------------------- | ------------------------------------ | ------- |
| `EW11_REBOOT_ENABLED`  | 주기 재부팅 사용 여부                | `false` |
| `EW11_REBOOT_HOST`     | 재부팅 요청 대상 호스트 (EW11 IP 등) | -       |
| `EW11_REBOOT_USERNAME` | EW11 HTTP 인증 아이디 (필수)         | -       |
| `EW11_REBOOT_PASSWORD` | EW11 HTTP 인증 비밀번호              | -       |
| `EW11_REBOOT_INTERVAL` | 재부팅 주기 (예: `12h`, `30m`, `1d`) | `12h`   |

예시:

```yaml
- EW11_REBOOT_ENABLED=true
- EW11_REBOOT_HOST=192.168.0.20
- EW11_REBOOT_USERNAME=id
- EW11_REBOOT_PASSWORD=pw
- EW11_REBOOT_INTERVAL=12h
```

```yml
volumes:
  - <마운트할 볼륨 명>:/app
```

위 볼륨은 **docker** 프로필 기본값으로 `/app` 에 생성되는 **H2 파일 DB**(`commax.mv.db` 등)를 컨테이너 재시작 후에도 유지하기 위해 사용합니다. 다른 경로에 두려면 `SPRING_DATASOURCE_URL=jdbc:h2:file:/원하는경로/이름` 으로 덮어쓰면 됩니다.

추후 Mariadb등의 db로 변경 가능성 있습니다

최초 실행시 initail.sql이 작동해야 합니다.

```sh
wallpadcontroller-1  | 2025-06-18T16:20:40.752Z  INFO 1 --- [Wallpadcontroller] [           main] c.h.w.initializer.StartUpRunner          : 🧩 device_type 비어 있음. commax-initial.sql 실행 시작
wallpadcontroller-1  | 2025-06-18T16:20:42.826Z  INFO 1 --- [Wallpadcontroller] [           main] c.h.w.initializer.StartUpRunner          : ✅ commax-initial.sql 실행 완료
```

위와 같은 로그가 찍히고 나면, docker컨테이너를 재실행해 주시면

```sh
wallpadcontroller-1  | 2025-06-18T16:22:26.316Z  INFO 1 --- [Wallpadcontroller] [           main] c.h.w.initializer.StartUpRunner          : ✅ 기존 device_type 데이터가 존재합니다. 초기화 스킵.
wallpadcontroller-1  | 2025-06-18T16:22:26.525Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Thermo_1 (index: 1)
wallpadcontroller-1  | 2025-06-18T16:22:26.532Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Thermo_2 (index: 2)
wallpadcontroller-1  | 2025-06-18T16:22:26.539Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Thermo_3 (index: 3)
wallpadcontroller-1  | 2025-06-18T16:22:26.546Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Thermo_4 (index: 4)
wallpadcontroller-1  | 2025-06-18T16:22:26.807Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Fan_1 (index: 1)
wallpadcontroller-1  | 2025-06-18T16:22:26.906Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Outlet_1 (index: 1)
wallpadcontroller-1  | 2025-06-18T16:22:27.108Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Outlet_2 (index: 2)
wallpadcontroller-1  | 2025-06-18T16:22:27.511Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_LightBreaker_1 (index: 1)
wallpadcontroller-1  | 2025-06-18T16:22:28.315Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Light_1 (index: 1)
wallpadcontroller-1  | 2025-06-18T16:22:28.412Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Light_2 (index: 2)
wallpadcontroller-1  | 2025-06-18T16:22:28.515Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Light_3 (index: 3)
wallpadcontroller-1  | 2025-06-18T16:22:28.611Z  INFO 1 --- [Wallpadcontroller] [lpad-controller] c.h.w.elfin.ElfinReceiveService          : 📥 등록된 새 기기: commax_Light_4 (index: 4)
```

위와 같이 sql init 스킵한다는 안내와 함계 기기를 등록하며 사용이 시작됩니다

# 특이사항

1. EV, Outlet구현 - 엘리베이터 기능 및 콘센트 기능은 미구현 상태입니다.
2. 화면 미구현 - 위 기능 안정화 이후 구현 예정이며, 기존 레퍼런스 프로젝트에 있던 기능중 필요한 기능 및 패킷 값 변경 등 추가적으로 필요한기능 추가 예정입니다.

## 난방 외출 상태

실기기에서 관측한 `82 84 03 28 23 00 00 54`의 상태 바이트 `84`를 외출로 처리합니다. ACK 헤더 `84`와는 별개입니다. 기존 DB에서 미등록 값이 원시 HEX `84`로 전달되는 경우와 새 DB의 `away` 매핑을 모두 지원합니다.

MQTT의 `commax/state/Thermo3/away`에 `ON`/`OFF`를 발행하고, HA에는 읽기 전용 `Thermo3 외출` binary sensor를 자동 등록합니다. 기본 토픽이 다르면 그 설정을 따릅니다. 일반/꺼짐 상태를 수신하면 외출 상태를 `OFF`로 되돌립니다. 외출 중 climate 모드는 `heat`를 유지하고 실제 가열 여부는 미확정이므로 action에 `None`을 보내 기존 가열 표시를 초기화합니다.

외출 송신 명령은 확인되지 않았으므로 추가하지 않습니다. 서비스 재시작 시 Discovery가 갱신됩니다.

## 난방 가동·유휴 표시 기준

실기기에서 상태 `81`은 가열 표시가 켜져 있을 때도 수신되어, 난방 ON으로 해석합니다. 일반 난방 ON(`81`, 기존 `83` 포함)에서는 설정온도가 현재온도보다 높으면 `action=heating`, 같거나 낮으면 `action=idle`을 발행합니다. 이는 온도에 따른 난방 수요 표시이며 보일러의 실제 연소 신호를 직접 읽은 값은 아닙니다.

꺼짐은 온도 차이와 관계없이 `action=off`, 외출은 기존 외출 표시를 유지하고 `action=None`입니다. 온도가 누락되거나 숫자로 해석되지 않으면 `None`으로 HA 가동 상태를 초기화합니다. 기존 DB의 `idle` 이름도 ON 상태로 처리하므로 DB 초기화 없이 적용됩니다. 변경 사항은 서비스 새 빌드·재시작 후 반영됩니다.
