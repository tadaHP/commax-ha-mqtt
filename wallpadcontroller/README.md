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
      - commax-wallpad:/data
    ports:
      - "52394:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/data/mydb.db
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
      - commax-wallpad:/data
    ports:
      - "52394:8080"
      - "54747:54747/udp"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/data/mydb.db
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
  - <마운트할 볼륨 명>:/data
```

위 내용은 /data 에 저장될 sqlite 정보를 지속해서 사용하기 위해 사용합니다

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
