package com.fabriciot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.common.result.Result;
import com.fabriciot.dto.audit.AuditListQuery;
import com.fabriciot.service.AuditService;
import com.fabriciot.vo.audit.AuditLogVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('audit:view')")
    public Result<List<AuditLogVO>> list(@Valid AuditListQuery query) {
        IPage<AuditLogVO> page = auditService.list(query);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @GetMapping("/{logId}")
    @PreAuthorize("hasAuthority('audit:view')")
    public Result<AuditLogVO> detail(@PathVariable("logId") String logId) {
        return Result.success(auditService.detail(logId));
    }
}

