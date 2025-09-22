package com.hyeonpyo.wallpadcontroller.controller;

import com.hyeonpyo.wallpadcontroller.service.UnknownPacketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class UnknownPacketController {

    private final UnknownPacketService unknownPacketService;

    @GetMapping("/unknown-packets")
    public String listUnknownPackets(
            Model model,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("packetPage", unknownPacketService.findAll(pageable));
        return "unknown-packets";
    }
}
