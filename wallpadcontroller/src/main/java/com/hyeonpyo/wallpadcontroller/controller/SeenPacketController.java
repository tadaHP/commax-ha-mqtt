package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SeenPacketController {

    @GetMapping("/seen-packets")
    public String seenPacketsPage() {
        return "seen-packets";
    }
}
