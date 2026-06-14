package com.hyeonpyo.wallpadcontroller.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyeonpyo.wallpadcontroller.dto.DevicePacketCoverageDeviceType;
import com.hyeonpyo.wallpadcontroller.service.PacketCoverageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coverage")
@RequiredArgsConstructor
public class PacketCoverageRestController {

    private final PacketCoverageService packetCoverageService;

    @GetMapping
    public List<DevicePacketCoverageDeviceType> getCoverage() {
        return packetCoverageService.getCoverageStatus();
    }
}
