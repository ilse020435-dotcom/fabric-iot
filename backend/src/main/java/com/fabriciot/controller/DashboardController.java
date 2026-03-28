package com.fabriciot.controller;

import com.fabriciot.common.result.Result;
import com.fabriciot.service.DashboardService;
import com.fabriciot.vo.dashboard.DashboardOverviewVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<DashboardOverviewVO> overview() {
        return Result.success(dashboardService.getOverview());
    }
}

