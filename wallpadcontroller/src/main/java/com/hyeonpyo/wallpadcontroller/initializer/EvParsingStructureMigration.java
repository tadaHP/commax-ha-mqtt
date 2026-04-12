package com.hyeonpyo.wallpadcontroller.initializer;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EV state(헤더 23): 데이터 바이트 1 = 호기(deviceId·기기 인덱스), 2 = 층(floor).
 * {@code parsing_field} / {@code parsing_field_value} 정렬과 EV 행의 {@code object_id} 보정만 수행합니다(멱등).
 * <p>
 * 과거 버그로 쌓인 {@code registered_devices} 오등록·HA discovery retain 은 기동마다 지우지 않습니다.
 * 1회만 정리가 필요하면 DB에서 수동 삭제하거나, 별도 스크립트를 사용하세요.
 */
@Slf4j
@Component("evParsingStructureMigration")
@RequiredArgsConstructor
@DependsOn("entityManagerFactory")
public class EvParsingStructureMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        try {
            Integer packetTypeId = jdbcTemplate.query(
                    "SELECT id FROM packet_type WHERE header = '23' AND kind = 'state'",
                    rs -> rs.next() ? rs.getInt(1) : null);
            if (packetTypeId == null) {
                return;
            }
            int p1 = jdbcTemplate.update(
                    "UPDATE parsing_field SET name = 'deviceId' WHERE packet_type_id = ? AND position = 1",
                    packetTypeId);
            int p2 = jdbcTemplate.update(
                    "UPDATE parsing_field SET name = 'floor' WHERE packet_type_id = ? AND position = 2",
                    packetTypeId);
            int p3 = jdbcTemplate.update(
                    "UPDATE parsing_field SET name = 'empty' WHERE packet_type_id = ? AND position = 3",
                    packetTypeId);
            int delVal = jdbcTemplate.update(
                    "DELETE FROM parsing_field_value WHERE parsing_field_id IN ("
                            + "SELECT id FROM parsing_field WHERE packet_type_id = ? AND position IN (1, 2))",
                    packetTypeId);
            int obj = jdbcTemplate.update(
                    "UPDATE registered_devices SET object_id = 'ev_status_' || index_number WHERE device_type = 'EV'");
            if (p1 > 0 || p2 > 0 || p3 > 0 || delVal > 0 || obj > 0) {
                log.info("EV 패킷 정의 보정: pos1~3 이름 {}+{}+{}, value 삭제 {}행, object_id {}행", p1, p2, p3, delVal, obj);
            }
        } catch (Exception e) {
            log.warn("EV parsing_field 마이그레이션 스킵 또는 실패(무시 가능): {}", e.getMessage());
        }
    }
}
