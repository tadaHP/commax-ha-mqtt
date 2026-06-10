package com.hyeonpyo.wallpadcontroller.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "packet-log.retention")
public class PacketLogRetentionProperties {
    /** 오래된 패킷 로그 자동 정리 사용 여부 */
    private boolean enabled = true;
    /** 보관 기간(일). 기본 14일. */
    private int days = 14;
    /** 한 번에 삭제할 row 수. 큰 DB에서 장시간 락을 피하기 위한 배치 크기. */
    private int batchSize = 5000;
}
