package com.fabriciot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.dto.monitor.MonitorListQuery;
import com.fabriciot.vo.monitor.MonitorRecordVO;
import com.fabriciot.vo.monitor.MonitorStatsVO;

public interface MonitorService {

    IPage<MonitorRecordVO> list(MonitorListQuery query);

    MonitorStatsVO stats();
}

