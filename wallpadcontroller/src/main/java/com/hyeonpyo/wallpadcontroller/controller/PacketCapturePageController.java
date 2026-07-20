package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hyeonpyo.wallpadcontroller.service.PacketTypeCatalogService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PacketCapturePageController {

    private final PacketTypeCatalogService packetTypeCatalogService;

    @GetMapping("/packet-capture")
    public String getPacketCapture(Model model) {
        model.addAttribute("packetTypeGroups", packetTypeCatalogService.getGroupedPacketTypes());
        return "packet-capture";
    }
}
