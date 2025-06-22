# Commax Wallpad - MQTT With Docker

이 프로젝트는 [@kimtc99](https://github.com/kimtc99/HAaddons)의 'CommaxWallpadBySaram' 애드온을 기반으로 작성된
[@wooooooooooook](https://github.com/wooooooooooook/HAaddons#)님의 애드온을 기반으로 작성되었으며
mqtt를 통해 elfin-ew11과 통신을 하여, HA에서 MQTT 통합 구성요소로 통합 가능하게 만든 특징이 있습니다.

기본 베이스 코드는 위 코드를 기반으로 진행하며, Spring을 통해 구동하도록 개발 예정입니다.

Docker로 HA를 이용하는 사용자들을 위한 프로젝트로
기존 MQTT 통합구성요소를 통해 Commax 월패드를 통합 예정입니다.

LICENSE는 AGPL 3.0 을  따라 자유로운 수전 및 재배포가 가능하나 소스코드 공개가 필수이며 상업적 사용도 가능하나 오픈소스로 유지하여야합니다.

# 사용법

사용 예제는 DockerCompose 기준으로 적용합니다

```yml
services:
  wallpadcontroller:
    image: ghcr.io/tadahp/commax-wallpad:latest
    volumes:
      - <마운트할 파일 명>:/app
    environment:
      - MQTT_HOST=localhost
      - MQTT_PORT=1883
      - MQTT_CLIENT_ID=wallpad-controller
      - MQTT_USERNAME=
      - MQTT_PASSWORD=
      - MQTT_HA_TOPIC=commax

volumes:
  <마운트할 파일 명>:
```

위 내용중 환경변수에 맞게 채워넣으시면 작동하며, 빈값은 옵션입니다.

```yml
volumes:
    - <마운트할 파일 명>:/app
```

위 내용은 /app 에 저장될 sqlite 정보를 지속해서 사용하기 위해 사용합니다

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
아직 개발진행중인 프로젝트이며, 아래와 같은 진행사항이 남아있습니다.

1. 상태 유지 로직 점검 - power가 아닌 다른 로직에서 상태 변경 이후 이를 감지하는 로직이 비정상적인 상태입니다.
2. EV, Outlet구현 - 엘리베이터 기능 및 콘센트 기능은 미구현 상태입니다.
3. 화면 미구현 - 위 기능 안정화 이후 구현 예정이며, 기존 레퍼런스 프로젝트에 있던 기능중 필요한 기능 및 패킷 값 변경 등 추가적으로 필요한기능 추가 예정입니다.
