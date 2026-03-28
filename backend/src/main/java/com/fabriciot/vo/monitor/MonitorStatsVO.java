package com.fabriciot.vo.monitor;

import lombok.Data;

import java.util.List;

@Data
public class MonitorStatsVO {

    private List<NameValueVO> distribution;
    private List<TrendVO> trend;

    @Data
    public static class NameValueVO {
        private String name;
        private Long value;
    }

    @Data
    public static class TrendVO {
        private String date;
        private Long online;
        private Long offline;
        private Long exception;
    }
}

