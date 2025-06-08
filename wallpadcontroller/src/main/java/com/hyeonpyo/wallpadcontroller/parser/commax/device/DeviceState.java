package com.hyeonpyo.wallpadcontroller.parser.commax.device;

import java.util.Map;

public interface DeviceState {
    String toJson();
    Map<String, String> toMap();
}
