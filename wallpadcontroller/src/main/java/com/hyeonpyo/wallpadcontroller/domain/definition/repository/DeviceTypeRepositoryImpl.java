package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

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
    public List<DeviceType> findAllWithFullStructure() {
        List<DeviceType> deviceTypes = entityManager.createQuery(
                        "SELECT DISTINCT dt FROM DeviceType dt LEFT JOIN FETCH dt.packetTypes",
                        DeviceType.class)
                .getResultList();

        if (deviceTypes.isEmpty()) {
            return deviceTypes;
        }

        List<PacketType> packetTypes = distinctPacketTypes(deviceTypes);
        if (!packetTypes.isEmpty()) {
            entityManager.createQuery(
                            "SELECT DISTINCT pt FROM PacketType pt LEFT JOIN FETCH pt.fields WHERE pt IN :packetTypes",
                            PacketType.class)
                    .setParameter("packetTypes", packetTypes)
                    .getResultList();
        }

        List<ParsingField> fields = distinctFields(packetTypes);
        if (!fields.isEmpty()) {
            entityManager.createQuery(
                            "SELECT DISTINCT f FROM ParsingField f LEFT JOIN FETCH f.valueMappings WHERE f IN :fields",
                            ParsingField.class)
                    .setParameter("fields", fields)
                    .getResultList();
        }

        return deviceTypes;
    }

    private static List<PacketType> distinctPacketTypes(List<DeviceType> deviceTypes) {
        Set<PacketType> unique = new LinkedHashSet<>();
        for (DeviceType deviceType : deviceTypes) {
            unique.addAll(deviceType.getPacketTypes());
        }
        return new ArrayList<>(unique);
    }

    private static List<ParsingField> distinctFields(List<PacketType> packetTypes) {
        Set<ParsingField> unique = new LinkedHashSet<>();
        for (PacketType packetType : packetTypes) {
            unique.addAll(packetType.getFields());
        }
        return new ArrayList<>(unique);
    }
}
