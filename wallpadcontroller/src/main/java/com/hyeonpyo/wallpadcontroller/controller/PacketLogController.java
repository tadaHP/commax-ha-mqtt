package com.hyeonpyo.wallpadcontroller.controller;

import com.hyeonpyo.wallpadcontroller.service.PacketLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PacketLogController {

    private final PacketLogService packetLogService;

    @GetMapping("/packet-logs")
    public String listPacketLogs(
            Model model,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("packetPage", packetLogService.findAll(pageable));
        return "packet-logs";
    }
}
