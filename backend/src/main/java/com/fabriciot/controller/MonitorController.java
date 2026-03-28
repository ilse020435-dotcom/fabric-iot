package com.fabriciot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.common.result.Result;
import com.fabriciot.dto.monitor.MonitorListQuery;
import com.fabriciot.service.MonitorService;
import com.fabriciot.vo.monitor.MonitorRecordVO;
import com.fabriciot.vo.monitor.MonitorStatsVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Result<List<MonitorRecordVO>> list(@Valid MonitorListQuery query) {
        IPage<MonitorRecordVO> page = monitorService.list(query);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('monitor:view')")
    public Result<MonitorStatsVO> stats() {
        return Result.success(monitorService.stats());
    }
}

