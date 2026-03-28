package com.fabriciot.vo.device;

import lombok.Data;

import java.util.List;

@Data
public class DeviceDetailVO {

    private DeviceListVO basic;
    private List<StatusRecordVO> statusRecords;
    private ChainSummaryVO chainSummary;
    private List<RecentAuditVO> recentAudits;

    @Data
    public static class StatusRecordVO {
        private String status;
        private String summaryHash;
        private Long blockHeight;
        private String txHash;
        private String time;
    }

    @Data
    public static class ChainSummaryVO {
        private Long latestBlockHeight;
        private String latestTxHash;
        private String chainHash;
        private String syncedAt;
    }

    @Data
    public static class RecentAuditVO {
        private String logId;
        private String operationType;
        private String operator;
        private String timestamp;
        private String remark;
    }
}

