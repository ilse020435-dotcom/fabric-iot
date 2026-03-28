package com.fabriciot.common.enums;

import java.util.Arrays;

public enum MonitorStatus {
    ONLINE("ONLINE", "在线"),
    OFFLINE("OFFLINE", "离线"),
    EXCEPTION("EXCEPTION", "异常");

    private final String code;
    private final String label;

    MonitorStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static MonitorStatus fromCodeOrLabel(String text) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(text) || item.label.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported monitor status: " + text));
    }
}
