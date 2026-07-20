package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PacketCoverageController {

    @GetMapping("/")
    public String coverageDashboard() {
        return "coverage";
    }
}
