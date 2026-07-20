package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.PacketType;
import com.hyeonpyo.wallpadcontroller.domain.definition.entity.ParsingField;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class DeviceTypeRepositoryImpl implements DeviceTypeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceType> findAllWithFullStructure() {
        List<DeviceType> deviceTypes = entityManager.createQuery(
                        "SELECT DISTINCT dt FROM DeviceType dt LEFT JOIN FETCH dt.packetTypes",
                        DeviceType.class)
                .getResultList();

        if (deviceTypes.isEmpty()) {
            return deviceTypes;
        }

        List<Long> packetTypeIds = distinctPacketTypeIds(deviceTypes);
        if (!packetTypeIds.isEmpty()) {
            entityManager.createQuery(
                            "SELECT DISTINCT pt FROM PacketType pt LEFT JOIN FETCH pt.fields WHERE pt.id IN :packetTypeIds",
                            PacketType.class)
                    .setParameter("packetTypeIds", packetTypeIds)
                    .getResultList();
        }

        if (!packetTypeIds.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Long> fieldIds = entityManager.createQuery(
                            "SELECT f.id FROM ParsingField f WHERE f.packetType.id IN :packetTypeIds")
                    .setParameter("packetTypeIds", packetTypeIds)
                    .getResultList();

            if (!fieldIds.isEmpty()) {
                entityManager.createQuery(
                                "SELECT DISTINCT f FROM ParsingField f LEFT JOIN FETCH f.valueMappings WHERE f.id IN :fieldIds",
                                ParsingField.class)
                        .setParameter("fieldIds", fieldIds)
                        .getResultList();
            }
        }

        return deviceTypes;
    }

    private static List<Long> distinctPacketTypeIds(List<DeviceType> deviceTypes) {
        Set<Long> unique = new LinkedHashSet<>();
        for (DeviceType deviceType : deviceTypes) {
            for (PacketType packetType : deviceType.getPacketTypes()) {
                unique.add(packetType.getId());
            }
        }
        return new ArrayList<>(unique);
    }
}
