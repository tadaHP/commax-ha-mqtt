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
    image: wallpadcontroller:latest
    environment:
      - MQTT_HOST=localhost
      - MQTT_PORT=1883
      - MQTT_CLIENT_ID=wallpad-controller
      - MQTT_USERNAME=
      - MQTT_PASSWORD=
      - MQTT_HA_TOPIC=commax
```

위 내용중 환경변수에 맞게 채워넣으시면 작동하며, 빈값은 옵션입니다.

# 특이사항
아직 개발진행중인 프로젝트이며, 아래와 같은 진행사항이 남아있습니다.

1. 상태 유지 로직 점검 - power가 아닌 다른 로직에서 상태 변경 이후 이를 감지하는 로직이 비정상적인 상태입니다.
2. EV, Outlet구현 - 엘리베이터 기능 및 콘센트 기능은 미구현 상태입니다.
3. 화면 미구현 - 위 기능 안정화 이후 구현 예정이며, 기존 레퍼런스 프로젝트에 있던 기능중 필요한 기능 및 패킷 값 변경 등 추가적으로 필요한기능 추가 예정입니다.
