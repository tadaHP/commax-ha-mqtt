package com.hyeonpyo.wallpadcontroller.domain.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "registered_devices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEntity {

    @Id
    @Column(name = "unique_id", nullable = false, unique = true)
    private String uniqueId; // Home Assistant 내부 고유 식별자

    @Column(name = "object_id", nullable = false)
    private String objectId; // entity_id의 이름이 되는 값

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceKey type; // light, switch, fan, etc.

    @Column(name = "index_number", nullable = false)
    private int index;
    
    // @Column(name = "manufacturer", nullable = false)
    // private String manufacturer; // 제조사 이름 (예: commax_wallpad)

}