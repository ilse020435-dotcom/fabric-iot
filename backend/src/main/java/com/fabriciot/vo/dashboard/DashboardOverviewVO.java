package com.fabriciot.vo.dashboard;

import lombok.Data;

import java.util.List;

@Data
public class DashboardOverviewVO {

    private StatsVO stats;
    private List<RecentOperationVO> recentOperations;
    private List<NameValueVO> statusDistribution;
    private List<TrendVO> accessTrend;

    @Data
    public static class StatsVO {
        private Long totalDevices;
        private Long onlineDevices;
        private Long frozenDevices;
        private Long deactivatedDevices;
        private Long newDevicesToday;
    }

    @Data
    public static class RecentOperationVO {
        private String operationId;
        private String operator;
        private String operationType;
        private String deviceId;
        private String timestamp;
    }

    @Data
    public static class NameValueVO {
        private String name;
        private Long value;
    }

    @Data
    public static class TrendVO {
        private String date;
        private Long value;
    }
}

