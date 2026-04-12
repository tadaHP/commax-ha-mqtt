package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hyeonpyo.wallpadcontroller.service.PacketLogService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class PacketLogController {

    private final PacketLogService packetLogService;

    /** 목록·검색·페이지네이션은 {@code GET /api/packet-logs} + 클라이언트 렌더링 */
    @GetMapping("/packet-logs")
    public String listPacketLogs() {
        return "packet-logs";
    }

    @GetMapping("/packet-capture")
    public String getPacketCapture(Model model) {
        model.addAttribute("packetTypeGroups", packetLogService.getGroupedPacketTypes());
        return "packet-capture";
    }
    
}
