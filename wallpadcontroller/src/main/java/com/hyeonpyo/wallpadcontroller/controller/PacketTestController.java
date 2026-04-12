package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PacketTestController {

    @GetMapping("/packet-test")
    public String packetTestPage() {
        return "packet-test";
    }
}
