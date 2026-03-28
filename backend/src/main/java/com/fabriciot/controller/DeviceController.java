package com.fabriciot.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.common.enums.LifecycleStatus;
import com.fabriciot.common.result.Result;
import com.fabriciot.dto.device.DeviceListQuery;
import com.fabriciot.dto.device.DeviceSaveRequest;
import com.fabriciot.dto.device.DeviceStatusReportRequest;
import com.fabriciot.dto.device.DeviceStatusUpdateRequest;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.security.SecurityUtils;
import com.fabriciot.service.DeviceImportService;
import com.fabriciot.service.DeviceService;
import com.fabriciot.vo.device.DeviceDetailVO;
import com.fabriciot.vo.device.DeviceImportResultVO;
import com.fabriciot.vo.device.DeviceImportTaskVO;
import com.fabriciot.vo.device.DeviceListVO;
import com.fabriciot.vo.device.LifecycleEventVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceImportService deviceImportService;

    public DeviceController(DeviceService deviceService, DeviceImportService deviceImportService) {
        this.deviceService = deviceService;
        this.deviceImportService = deviceImportService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('device:list')")
    public Result<List<DeviceListVO>> list(@Valid DeviceListQuery query) {
        IPage<DeviceListVO> page = deviceService.list(query);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('device:list')")
    public Result<DeviceDetailVO> detail(@PathVariable("id") String deviceId) {
        return Result.success(deviceService.detail(deviceId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('device:create')")
    public Result<DeviceListVO> create(@Valid @RequestBody DeviceSaveRequest request) {
        return Result.success(deviceService.create(request, SecurityUtils.currentUsername()));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('device:import')")
    public Result<DeviceImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success(deviceImportService.importExcel(file, SecurityUtils.currentUsername()));
    }

    @PostMapping("/import/task")
    @PreAuthorize("hasAuthority('device:import')")
    public Result<DeviceImportTaskVO> startImportTask(@RequestParam("file") MultipartFile file) {
        return Result.success(deviceImportService.startImportTask(file, SecurityUtils.currentUsername()));
    }

    @GetMapping("/import/task/{taskId}")
    @PreAuthorize("hasAuthority('device:import')")
    public Result<DeviceImportTaskVO> queryImportTask(@PathVariable("taskId") String taskId) {
        return Result.success(deviceImportService.queryImportTask(taskId, SecurityUtils.currentUsername()));
    }

    @GetMapping("/import/template")
    @PreAuthorize("hasAuthority('device:import')")
    public void downloadImportTemplate(HttpServletResponse response) throws IOException {
        byte[] bytes = deviceImportService.exportTemplate();
        String encodedFileName = URLEncoder.encode("device-import-template.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setContentLength(bytes.length);
        StreamUtils.copy(bytes, response.getOutputStream());
        response.flushBuffer();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('device:update')")
    public Result<DeviceListVO> update(@PathVariable("id") String deviceId, @Valid @RequestBody DeviceSaveRequest request) {
        return Result.success(deviceService.update(deviceId, request, SecurityUtils.currentUsername()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('device:update','device:freeze','device:revoke')")
    public Result<DeviceListVO> updateStatus(@PathVariable("id") String deviceId,
                                             @Valid @RequestBody DeviceStatusUpdateRequest request) {
        validateStatusAuthority(request.getLifecycleStatus());
        return Result.success(deviceService.changeStatus(deviceId, request.getLifecycleStatus(), SecurityUtils.currentUsername()));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('device:update')")
    public Result<DeviceListVO> activate(@PathVariable("id") String deviceId) {
        return Result.success(deviceService.activate(deviceId, SecurityUtils.currentUsername()));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasAuthority('device:freeze')")
    public Result<DeviceListVO> freeze(@PathVariable("id") String deviceId) {
        return Result.success(deviceService.freeze(deviceId, SecurityUtils.currentUsername()));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('device:revoke')")
    public Result<DeviceListVO> revoke(@PathVariable("id") String deviceId) {
        return Result.success(deviceService.revoke(deviceId, SecurityUtils.currentUsername()));
    }

    @GetMapping("/{id}/lifecycle")
    @PreAuthorize("hasAuthority('device:list')")
    public Result<List<LifecycleEventVO>> lifecycle(@PathVariable("id") String deviceId,
                                                    @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(deviceService.lifecycleRecords(deviceId, limit));
    }

    @PostMapping("/{id}/status/report")
    @PreAuthorize("hasAnyAuthority('monitor:view','device:update')")
    public Result<Void> reportStatus(@PathVariable("id") String deviceId,
                                     @Valid @RequestBody DeviceStatusReportRequest request) {
        deviceService.reportStatus(deviceId, request, SecurityUtils.currentUsername());
        return Result.success(null);
    }

    private void validateStatusAuthority(String lifecycleStatus) {
        LifecycleStatus target = LifecycleStatus.fromCodeOrLabel(lifecycleStatus);
        boolean allowed = switch (target) {
            case ACTIVATED -> SecurityUtils.hasAuthority("device:update");
            case FROZEN -> SecurityUtils.hasAuthority("device:freeze");
            case REVOKED -> SecurityUtils.hasAuthority("device:revoke");
            case REGISTERED -> false;
        };
        if (!allowed) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前账号无权执行该状态变更");
        }
    }
}

