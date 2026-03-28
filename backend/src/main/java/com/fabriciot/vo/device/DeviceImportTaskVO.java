package com.fabriciot.vo.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceImportTaskVO {

    private String taskId;
    private String status;
    private String stage;
    private Integer totalRows;
    private Integer processedRows;
    private Integer successCount;
    private Integer failedCount;
    private Integer progressPercent;
    private String message;
    private Long startedAt;
    private Long finishedAt;
    private DeviceImportResultVO result;
}
