package com.hyeonpyo.wallpadcontroller.domain.builder.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hyeonpyo.wallpadcontroller.domain.builder.CommandBuilder;
import com.hyeonpyo.wallpadcontroller.domain.builder.FanCommandBuilder;
import com.hyeonpyo.wallpadcontroller.domain.builder.SimpleCommandBuilder;

@Configuration
public class CommandBuilderConfig {

    @Bean
    public Map<String, CommandBuilder> commandBuilderMap(
        SimpleCommandBuilder simpleBuilder,
        FanCommandBuilder fanBuilder
    ) {
        Map<String, CommandBuilder> map = new HashMap<>();
        map.put("light", simpleBuilder);
        map.put("outlet", simpleBuilder);
        map.put("gas", simpleBuilder);
        map.put("fan", fanBuilder);
        // 추가적으로 thermoBuilder도 나중에 넣을 수 있음
        return map;
    }
}
