package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingFieldValue;


public interface ParsingFieldValueRepository extends JpaRepository<ParsingFieldValue, Long> {
    @Query("""
        SELECT p.hex
        FROM ParsingFieldValue p
        WHERE p.parsingField = :field AND p.rawKey = :rawKey
    """)
    Optional<String> findHexByParsingFieldAndRawKey(@Param("field") ParsingField field, @Param("rawKey") String rawKey);
}