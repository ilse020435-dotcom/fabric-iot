package com.fabriciot.common.enums;

import java.util.Arrays;

public enum PermissionType {
    MENU,
    BUTTON,
    API;

    public static PermissionType fromText(String text) {
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported permission type: " + text));
    }
}

