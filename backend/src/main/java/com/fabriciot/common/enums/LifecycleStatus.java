package com.fabriciot.common.enums;

import java.util.Arrays;

public enum LifecycleStatus {
    REGISTERED("REGISTERED", "已注册"),
    ACTIVATED("ACTIVATED", "已激活"),
    FROZEN("FROZEN", "已冻结"),
    REVOKED("REVOKED", "已注销");

    private final String code;
    private final String label;

    LifecycleStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static LifecycleStatus fromCodeOrLabel(String text) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(text) || item.label.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported lifecycle status: " + text));
    }
}
