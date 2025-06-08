package com.hyeonpyo.wallpadcontroller.domain.builder;

import java.util.Optional;

public interface CommandBuilder {
    Optional<byte[]> build(String type, int index, String field, String payload);
}
