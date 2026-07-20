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

## seen_packet (관측된 유니크 패킷)

SUCCESS 수신 패킷과 앱에서 EW11로 송신한 패킷의 **raw hex 전체**를 방향별(`INBOUND`/`OUTBOUND`)로 유니크하게 `seen_packet` 테이블과 메모리(`SeenPacketMemoryStore`)에 보관합니다. 이미 본 패킷은 DB insert 없이 메모리 `lastSeenAt`만 갱신합니다. 기존 데이터는 마이그레이션 시 `INBOUND`로 간주합니다. **자동 삭제·rotation은 없습니다.**

- 커버리지(`/`, `/api/coverage`): 메모리의 `INBOUND` seen 패킷을 파싱해 필드별 수신 현황 집계
- 관측 패킷 페이지: `GET /seen-packets`, `GET /api/seen-packets`
- 실시간 캡처(`/packet-capture`): SSE로만 표시, DB에 저장하지 않음

Docker 포트 매핑 예시는 루트 README의 compose 스니펫을 참고하면 됩니다.
