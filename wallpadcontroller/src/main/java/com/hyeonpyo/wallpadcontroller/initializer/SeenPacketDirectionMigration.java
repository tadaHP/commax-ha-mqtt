package com.hyeonpyo.wallpadcontroller.initializer;

import java.util.List;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("seenPacketDirectionMigration")
@RequiredArgsConstructor
@DependsOn("entityManagerFactory")
public class SeenPacketDirectionMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        try {
            jdbcTemplate.update("UPDATE seen_packet SET direction = 'INBOUND' WHERE direction IS NULL");
            dropRawDataOnlyUniqueConstraints();
        } catch (Exception e) {
            log.warn("seen_packet direction 마이그레이션 스킵 또는 실패(무시 가능): {}", e.getMessage());
        }
    }

    private void dropRawDataOnlyUniqueConstraints() {
        List<String> constraintNames = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                  ON tc.constraint_catalog = kcu.constraint_catalog
                 AND tc.constraint_schema = kcu.constraint_schema
                 AND tc.constraint_name = kcu.constraint_name
                WHERE UPPER(tc.table_name) = 'SEEN_PACKET'
                  AND tc.constraint_type = 'UNIQUE'
                GROUP BY tc.constraint_name
                HAVING COUNT(*) = 1
                   AND MAX(UPPER(kcu.column_name)) = 'RAW_DATA'
                """, String.class);

        for (String constraintName : constraintNames) {
            jdbcTemplate.execute("ALTER TABLE seen_packet DROP CONSTRAINT " + constraintName);
            log.info("seen_packet raw_data 단독 unique 제약 제거: {}", constraintName);
        }
    }
}
