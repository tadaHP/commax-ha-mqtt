package com.hyeonpyo.wallpadcontroller.domain.coverage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seen_packet",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seen_packet_raw_direction", columnNames = {"raw_data", "direction"})
        },
        indexes = {
                @Index(name = "idx_seen_packet_header", columnList = "header"),
                @Index(name = "idx_seen_packet_direction", columnList = "direction"),
                @Index(name = "idx_seen_packet_last_seen_at", columnList = "last_seen_at")
        })
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeenPacket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_data", nullable = false, length = 32)
    private String rawData;

    @Column(nullable = false, length = 2)
    private String header;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16, columnDefinition = "varchar(16) default 'INBOUND'")
    private SeenPacketDirection direction;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;
}
