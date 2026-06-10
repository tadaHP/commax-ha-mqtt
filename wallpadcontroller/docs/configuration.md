# 설정 요약

원본: `src/main/resources/application.yml`. 배포 시에는 환경변수로 덮어쓰는 경우가 많습니다.

## MQTT (HA·브로커 공용 클라이언트)

| 키 / 환경변수 | 설명 | 기본 예시 |
|---------------|------|-----------|
| `mqtt.host` / `MQTT_HOST` | 브로커 호스트 | `localhost` |
| `mqtt.port` / `MQTT_PORT` | 포트 | `1883` |
| `mqtt.client-id` / `MQTT_CLIENT_ID` | 클라이언트 ID | `wallpad-controller` |
| `mqtt.username` / `MQTT_USERNAME` | 인증 (선택) | 빈 값 |
| `mqtt.password` / `MQTT_PASSWORD` | 인증 (선택) | 빈 값 |
| `mqtt.ha-topic` / `MQTT_HA_TOPIC` | HA 상태·명령 토픽 prefix | `commax` |

앱은 이 브로커에 연결한 뒤:

- 구독: `{ha-topic}/command/#`, 그리고 EW11 **MQTT 모드**일 때 `ew11.receive-topic`
- 발행: 상태·discovery·(MQTT 모드) `ew11.send-topic` 등

## EW11

| 키 / 환경변수 | 설명 |
|---------------|------|
| `ew11.transport` / `EW11_TRANSPORT` | `mqtt` 또는 `udp` |
| `ew11.mqtt.send-topic` / `EW11_MQTT_SEND_TOPIC` | EW11로 보낼 publish 토픽 |
| `ew11.mqtt.receive-topic` / `EW11_MQTT_RECEIVE_TOPIC` | EW11에서 오는 구독 토픽 |
| `ew11.udp.send.host` / `EW11_UDP_SEND_HOST` | UDP 송신 대상 IP |
| `ew11.udp.send.port` / `EW11_UDP_SEND_PORT` | UDP 송신 포트 |
| `ew11.udp.listen.port` / `EW11_UDP_LISTEN_PORT` | UDP 수신 바인 포트 |
| `ew11.udp.listen.buffer-size` / `EW11_UDP_BUFFER_SIZE` | 수신 버퍼 크기 |

## EW11 주기 재부팅 (HTTP)

| 환경변수 | 설명 |
|----------|------|
| `EW11_REBOOT_ENABLED` | `true`일 때만 동작 |
| `EW11_REBOOT_HOST` | EW11 웹 UI 호스트 |
| `EW11_REBOOT_USERNAME` / `EW11_REBOOT_PASSWORD` | HTTP 인증 |
| `EW11_REBOOT_INTERVAL` | 주기 (예: `12h`, `30m`) |

## 데이터소스

| 키 / 환경변수 | 설명 |
|---------------|------|
| `spring.datasource.url` / `SPRING_DATASOURCE_URL` | H2 JDBC URL만 허용 (docker 기본: `jdbc:h2:file:/app/commax`). 예전 SQLite URL(`jdbc:sqlite:...`)을 넣으면 H2 드라이버와 불일치로 기동 실패. 미설정 시 docker는 기본 파일 URL 사용. |

## 패킷 로그 보존

패킷 로그는 기본적으로 14일만 보관하고, 매일 03:30(Asia/Seoul)에 오래된 로그를 배치 단위로 삭제합니다.

| 키 / 환경변수 | 설명 | 기본값 |
|---------------|------|--------|
| `packet-log.retention.enabled` / `PACKET_LOG_RETENTION_ENABLED` | 자동 정리 사용 여부 | `true` |
| `packet-log.retention.days` / `PACKET_LOG_RETENTION_DAYS` | 보관 기간(일) | `14` |
| `packet-log.retention.batch-size` / `PACKET_LOG_RETENTION_BATCH_SIZE` | 한 번에 삭제할 row 수 | `5000` |
| `packet-log.retention.cleanup-cron` / `PACKET_LOG_RETENTION_CLEANUP_CRON` | 정리 스케줄 cron | `0 30 3 * * *` |
| `packet-log.retention.zone` / `PACKET_LOG_RETENTION_ZONE` | cron 기준 시간대 | `Asia/Seoul` |

Docker 포트 매핑 예시는 루트 README의 compose 스니펫을 참고하면 됩니다.
