package com.fabriciot.common.enums;

import java.util.Arrays;

public enum OperationType {
    REGISTER("REGISTER", "注册设备"),
    ACTIVATE("ACTIVATE", "激活设备"),
    FREEZE("FREEZE", "冻结设备"),
    REVOKE("REVOKE", "注销设备"),
    UPDATE("UPDATE", "更新设备信息"),
    SYNC_STATUS("SYNC_STATUS", "同步状态摘要"),
    QUERY("QUERY", "查询");

    private final String code;
    private final String label;

    OperationType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static OperationType fromCodeOrLabel(String text) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(text) || item.label.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported operation type: " + text));
    }

    public static String labelOf(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .map(OperationType::getLabel)
                .orElse(code);
    }
}
