package com.fabriciot.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class IdGeneratorUtil {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private IdGeneratorUtil() {
    }

    public static String nextDeviceId() {
        return "DEV-" + TIME_FORMATTER.format(LocalDateTime.now()) + String.format("%03d", nextSeq());
    }

    public static String nextDid(String deviceId) {
        return "did:fabric:iot:" + deviceId.toLowerCase();
    }

    public static String nextAuditLogId() {
        return "AUD-" + TIME_FORMATTER.format(LocalDateTime.now()) + String.format("%03d", nextSeq());
    }

    public static String fallbackTxHash(String seed) {
        return HashUtil.sha256Hex(seed + ":" + UUID.randomUUID());
    }

    private static int nextSeq() {
        int value = SEQ.incrementAndGet();
        if (value > 999) {
            SEQ.set(0);
            return 0;
        }
        return value;
    }
}

