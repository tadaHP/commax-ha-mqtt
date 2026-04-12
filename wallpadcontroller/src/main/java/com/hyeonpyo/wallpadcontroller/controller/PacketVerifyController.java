package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PacketVerifyController {

    @GetMapping("/packet-verify")
    public String packetVerify() {
        return "packet-verify";
    }
}
