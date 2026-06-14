package com.hyeonpyo.wallpadcontroller.domain.definition.repository;

import java.util.List;

import com.hyeonpyo.wallpadcontroller.domain.definition.entity.DeviceType;

public interface DeviceTypeRepositoryCustom {

    List<DeviceType> findAllWithFullStructure();
}
