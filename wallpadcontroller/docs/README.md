# Wallpad Controller 문서

코맥스 월패드 ↔ **EW11(Elfin)** ↔ 이 애플리케이션 ↔ **MQTT 브로커** ↔ **Home Assistant** 흐름을 기준으로 정리했습니다.

## 문서 목차

| 문서 | 내용 |
|------|------|
| [architecture.md](./architecture.md) | 전체 구조, 주요 컴포넌트, MQTT/UDP 분기 |
| [data-flow.md](./data-flow.md) | 월패드 상태 수신·파싱·HA 발행, HA 명령·EW11 송신 시퀀스 |
| [configuration.md](./configuration.md) | `application.yml` / 환경변수 요약 |
| [operations-and-web.md](./operations-and-web.md) | 기동·DB 초기화, 내장 웹 UI 경로 |

루트 [README.md](../README.md)에는 Docker 예시와 사용법이 있습니다. 여기서는 **코드·런타임 관점**을 보강합니다.
