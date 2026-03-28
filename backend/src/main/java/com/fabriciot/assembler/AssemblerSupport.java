package com.fabriciot.assembler;

import com.fabriciot.common.enums.ChainStatus;
import com.fabriciot.common.enums.LifecycleStatus;
import com.fabriciot.common.enums.MonitorStatus;
import com.fabriciot.common.enums.OperationType;
import com.fabriciot.common.enums.UserStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AssemblerSupport {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String format(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.format(DATETIME_FORMATTER);
    }

    public String lifecycleLabel(String code) {
        return LifecycleStatus.fromCodeOrLabel(code).getLabel();
    }

    public String monitorLabel(String code) {
        return MonitorStatus.fromCodeOrLabel(code).getLabel();
    }

    public String chainLabel(String code) {
        return ChainStatus.fromCodeOrLabel(code).getLabel();
    }

    public String operationLabel(String code) {
        return OperationType.labelOf(code);
    }

    public String onChainLabel(Integer onChain) {
        return onChain != null && onChain == 1 ? "已上链" : "未上链";
    }

    public String userStatusLabel(String code) {
        return UserStatus.fromCode(code).getLabel();
    }
}
