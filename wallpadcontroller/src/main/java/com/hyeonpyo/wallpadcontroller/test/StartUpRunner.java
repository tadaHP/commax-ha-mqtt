package com.hyeonpyo.wallpadcontroller.test;

import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hyeonpyo.wallpadcontroller.parser.DeviceStructureLoader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartUpRunner implements CommandLineRunner{

    private final DeviceStructureLoader deviceStructureLoader;

    @Override
    public void run(String... args) throws Exception {
        deviceStructureLoader.loadDeviceStructure();

        Map<String, Object> structure = deviceStructureLoader.getDeviceStructure();
        structure.keySet().forEach(name -> System.out.println("로드된 기기: " + name));
    }
    
}
