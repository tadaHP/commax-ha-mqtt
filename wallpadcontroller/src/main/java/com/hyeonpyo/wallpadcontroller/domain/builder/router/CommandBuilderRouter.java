package com.hyeonpyo.wallpadcontroller.domain.builder.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.hyeonpyo.wallpadcontroller.domain.builder.CommandBuilder;
import com.hyeonpyo.wallpadcontroller.domain.builder.FanCommandBuilder;
import com.hyeonpyo.wallpadcontroller.domain.builder.SimpleCommandBuilder;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Primary
@Service
@RequiredArgsConstructor
public class CommandBuilderRouter implements CommandBuilder {

    private final SimpleCommandBuilder simpleBuilder;
    private final List<CommandBuilder> builders; // @Component로 등록된 모든 구현체
    private final Map<String, CommandBuilder> builderMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (CommandBuilder builder : builders) {
            if (builder instanceof FanCommandBuilder) {
                builderMap.put("fan", builder);
            }
        }
    }

    @Override
    public Optional<byte[]> build(String type, int index, String field, String payload) {
        CommandBuilder selected = builderMap.getOrDefault(type.toLowerCase(), simpleBuilder);
        return selected.build(type, index, field, payload);
    }
}
