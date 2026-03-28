package com.fabriciot.vo.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceImportResultVO {

    private Integer totalRows;
    private Integer successCount;
    private Integer failedCount;
    private List<String> createdDeviceIds;
    private List<ImportFailureVO> failures;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportFailureVO {
        private Integer rowNumber;
        private String deviceId;
        private String deviceName;
        private String message;
    }
}
