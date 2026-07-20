package com.hyeonpyo.wallpadcontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Web entry point for operational settings; device changes use DeviceController's API. */
@Controller
public class SettingsPageController {
    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
