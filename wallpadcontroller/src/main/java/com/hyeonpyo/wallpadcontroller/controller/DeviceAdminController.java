package com.hyeonpyo.wallpadcontroller.controller;

import java.util.List;
import java.util.NoSuchElementException;

import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntity;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hyeonpyo.wallpadcontroller.domain.device.DeviceEntityRepository;
import com.hyeonpyo.wallpadcontroller.domain.device.DeviceRegistryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DeviceAdminController {

    private final DeviceEntityRepository deviceEntityRepository;
    private final DeviceRegistryService deviceRegistryService;

    private static final String USED_PARAM = "used";

    @GetMapping("/devices")
    public String list(
            @RequestParam(name = USED_PARAM, defaultValue = "all") String usedFilter,
            Model model) {
        Sort sort = Sort.by(Sort.Order.asc("type"), Sort.Order.asc("index"));
        String normalized = normalizeUsedFilter(usedFilter);
        List<DeviceEntity> devices =
                switch (normalized) {
                    case "active" -> deviceEntityRepository.findAllByUsedTrue(sort);
                    case "inactive" -> deviceEntityRepository.findAllByUsedFalse(sort);
                    case "null" -> deviceEntityRepository.findAllByUsedIsNull(sort);
                    default -> deviceEntityRepository.findAll(sort);
                };
        model.addAttribute("devices", devices);
        model.addAttribute("filterUsed", normalized);
        return "devices";
    }

    /** 쿼리 파라미터 {@code used}: {@code all} | {@code active} | {@code inactive} | {@code null} */
    private static String normalizeUsedFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "all";
        }
        return switch (raw.trim().toLowerCase()) {
            case "active", "on", "true" -> "active";
            case "inactive", "off", "false" -> "inactive";
            case "null", "legacy" -> "null";
            default -> "all";
        };
    }

    /**
     * 폼 POST 폴백. 화면은 주로 {@code PUT /api/devices/mqtt-mode} 를 사용합니다.
     *
     * @param mqttState {@code on} / {@code off} / {@code legacy}
     */
    @PostMapping("/devices/used")
    public String updateUsed(
            @RequestParam String uniqueId,
            @RequestParam String mqttState,
            @RequestParam(name = USED_PARAM, defaultValue = "all") String usedFilter,
            RedirectAttributes redirectAttributes) {
        try {
            deviceRegistryService.updateMqttPublicationMode(uniqueId, mqttState);
            redirectAttributes.addFlashAttribute("message", "저장했습니다.");
        } catch (NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("error", "기기를 찾을 수 없습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/devices?" + USED_PARAM + "=" + normalizeUsedFilter(usedFilter);
    }
}
