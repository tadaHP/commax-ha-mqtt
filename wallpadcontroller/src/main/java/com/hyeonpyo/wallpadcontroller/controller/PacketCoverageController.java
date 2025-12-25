package com.hyeonpyo.wallpadcontroller.controller;

import com.hyeonpyo.wallpadcontroller.service.PacketCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PacketCoverageController {

    private final PacketCoverageService packetCoverageService;

    @GetMapping("/")
    public String coverageDashboard(Model model) throws Exception {
        model.addAttribute("devices", packetCoverageService.getCoverageStatus());
        return "coverage";
    }
}
