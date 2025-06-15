package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.CommandMappingRule;

public interface CommandMappingRuleRepository extends JpaRepository<CommandMappingRule, Long> {

    @Query("""
        SELECT r FROM CommandMappingRule r
        JOIN FETCH r.deviceType dt
        LEFT JOIN FETCH r.details d
        WHERE dt.name = :type
          AND r.externalField = :field
          AND (r.externalPayload = :payload OR r.externalPayload IS NULL)
    """)
    Optional<CommandMappingRule> findWithDetailsByDeviceTypeNameAndExternalFieldAndExternalPayload(
            @Param("type") String type,
            @Param("field") String field,
            @Param("payload") String payload
    );
}
