package com.fabriciot.common.enums;

import java.util.Arrays;

public enum ChainStatus {
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String label;

    ChainStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ChainStatus fromCodeOrLabel(String text) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(text) || item.label.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported chain status: " + text));
    }
}
