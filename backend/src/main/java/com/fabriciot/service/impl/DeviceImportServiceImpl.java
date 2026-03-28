package com.fabriciot.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fabriciot.dto.device.DeviceImportExcelRow;
import com.fabriciot.dto.device.DeviceSaveRequest;
import com.fabriciot.exception.BizException;
import com.fabriciot.exception.ErrorCode;
import com.fabriciot.service.DeviceImportService;
import com.fabriciot.service.DeviceService;
import com.fabriciot.vo.device.DeviceImportResultVO;
import com.fabriciot.vo.device.DeviceImportTaskVO;
import com.fabriciot.vo.device.DeviceListVO;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceImportServiceImpl implements DeviceImportService {

    private static final long TASK_EXPIRE_MILLIS = TimeUnit.HOURS.toMillis(2);

    private final DeviceService deviceService;
    private final ExecutorService importExecutor = Executors.newFixedThreadPool(2);
    private final ConcurrentMap<String, ImportTaskContext> taskMap = new ConcurrentHashMap<>();

    public DeviceImportServiceImpl(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PreDestroy
    public void shutdown() {
        importExecutor.shutdown();
    }

    @Override
    public DeviceImportResultVO importExcel(MultipartFile file, String operator) {
        validateFile(file);
        List<ImportRow> rows = parseRows(file);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Excel has no data rows");
        }
        return executeImport(rows, operator, null);
    }

    @Override
    public DeviceImportTaskVO startImportTask(MultipartFile file, String operator) {
        validateFile(file);
        byte[] fileBytes = toFileBytes(file);
        cleanupExpiredTasks();

        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportTaskContext context = ImportTaskContext.pending(taskId, operator);
        taskMap.put(taskId, context);

        importExecutor.submit(() -> runImportTask(taskId, operator, fileBytes));
        return context.toVO();
    }

    @Override
    public DeviceImportTaskVO queryImportTask(String taskId, String operator) {
        cleanupExpiredTasks();
        ImportTaskContext context = taskMap.get(taskId);
        if (context == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Import task not found");
        }
        if (!StringUtils.equals(context.getOperator(), operator)) {
            throw new BizException(ErrorCode.FORBIDDEN, "No permission to access this import task");
        }
        return context.toVO();
    }

    @Override
    public byte[] exportTemplate() {
        DeviceImportExcelRow sample = new DeviceImportExcelRow();
        sample.setDeviceName("sample-device");
        sample.setDeviceType("sensor");
        sample.setVendor("sample-vendor");
        sample.setDescription("optional");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcel.write(outputStream, DeviceImportExcelRow.class)
                    .sheet("device_import_template")
                    .doWrite(List.of(sample));
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "Failed to generate template");
        }
    }

    private void runImportTask(String taskId, String operator, byte[] fileBytes) {
        ImportTaskContext context = taskMap.get(taskId);
        if (context == null) {
            return;
        }

        try {
            context.markRunning("PARSING", "Parsing Excel...");
            List<ImportRow> rows = parseRows(fileBytes);
            if (rows.isEmpty()) {
                context.markFailed("Excel has no data rows", null);
                return;
            }

            context.markRunning("IMPORTING", "Importing devices...");
            context.setTotalRows(rows.size());
            DeviceImportResultVO result = executeImport(rows, operator, context::updateProgress);
            context.markSuccess(result, "Import completed");
        } catch (Exception ex) {
            context.markFailed(extractMessage(ex), null);
        }
    }

    private DeviceImportResultVO executeImport(List<ImportRow> rows, String operator, ProgressCallback callback) {
        List<String> createdDeviceIds = new ArrayList<>();
        List<DeviceImportResultVO.ImportFailureVO> failures = new ArrayList<>();
        int successCount = 0;
        int processedRows = 0;

        for (ImportRow row : rows) {
            DeviceSaveRequest request = toSaveRequest(row.data());
            String validationError = validateRow(row.rowNumber(), request);
            if (StringUtils.isNotBlank(validationError)) {
                failures.add(DeviceImportResultVO.ImportFailureVO.builder()
                        .rowNumber(row.rowNumber())
                        .deviceName(request.getDeviceName())
                        .message(validationError)
                        .build());
                processedRows++;
                notifyProgress(callback, rows.size(), processedRows, successCount, failures.size());
                continue;
            }

            try {
                DeviceListVO created = deviceService.create(request, operator);
                createdDeviceIds.add(created.getDeviceId());
                successCount++;
            } catch (Exception ex) {
                failures.add(DeviceImportResultVO.ImportFailureVO.builder()
                        .rowNumber(row.rowNumber())
                        .deviceName(request.getDeviceName())
                        .message(extractMessage(ex))
                        .build());
            }

            processedRows++;
            notifyProgress(callback, rows.size(), processedRows, successCount, failures.size());
        }

        return DeviceImportResultVO.builder()
                .totalRows(rows.size())
                .successCount(successCount)
                .failedCount(failures.size())
                .createdDeviceIds(createdDeviceIds)
                .failures(failures)
                .build();
    }

    private void notifyProgress(ProgressCallback callback, int totalRows, int processedRows, int successCount, int failedCount) {
        if (callback == null) {
            return;
        }
        callback.update(totalRows, processedRows, successCount, failedCount);
    }

    private byte[] toFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "Failed to read uploaded file");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Please upload an Excel file");
        }
        String filename = StringUtils.defaultString(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Only .xlsx or .xls is supported");
        }
    }

    private List<ImportRow> parseRows(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return readRows(inputStream);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "Failed to read Excel file");
        } catch (RuntimeException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "Failed to parse Excel file");
        }
    }

    private List<ImportRow> parseRows(byte[] fileBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            return readRows(inputStream);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "Failed to read Excel file");
        } catch (RuntimeException ex) {
            throw new BizException(ErrorCode.BIZ_ERROR, "Failed to parse Excel file");
        }
    }

    private List<ImportRow> readRows(InputStream inputStream) {
        ImportRowListener listener = new ImportRowListener();
        EasyExcel.read(inputStream, DeviceImportExcelRow.class, listener)
                .sheet()
                .headRowNumber(1)
                .doRead();
        return listener.rows();
    }

    private DeviceSaveRequest toSaveRequest(DeviceImportExcelRow row) {
        DeviceSaveRequest request = new DeviceSaveRequest();
        request.setDeviceId(null);
        request.setDid(null);
        request.setDeviceName(StringUtils.trimToNull(row.getDeviceName()));
        request.setDeviceType(StringUtils.trimToNull(row.getDeviceType()));
        request.setVendor(StringUtils.trimToNull(row.getVendor()));
        request.setDescription(StringUtils.trimToNull(row.getDescription()));
        return request;
    }

    private String validateRow(int rowNumber, DeviceSaveRequest request) {
        List<String> missing = new ArrayList<>();
        if (StringUtils.isBlank(request.getDeviceName())) {
            missing.add("deviceName");
        }
        if (StringUtils.isBlank(request.getDeviceType())) {
            missing.add("deviceType");
        }
        if (StringUtils.isBlank(request.getVendor())) {
            missing.add("vendor");
        }
        if (missing.isEmpty()) {
            return null;
        }
        return "Row " + rowNumber + " missing required fields: " + String.join(", ", missing);
    }

    private String extractMessage(Exception ex) {
        if (ex instanceof BizException bizException) {
            return bizException.getMessage();
        }
        return StringUtils.defaultIfBlank(ex.getMessage(), "Import failed");
    }

    private void cleanupExpiredTasks() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ImportTaskContext> entry : taskMap.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                taskMap.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private interface ProgressCallback {
        void update(int totalRows, int processedRows, int successCount, int failedCount);
    }

    private record ImportRow(int rowNumber, DeviceImportExcelRow data) {
    }

    private static final class ImportRowListener extends AnalysisEventListener<DeviceImportExcelRow> {
        private final List<ImportRow> rows = new ArrayList<>();

        @Override
        public void invoke(DeviceImportExcelRow data, AnalysisContext context) {
            int rowNumber = context.readRowHolder().getRowIndex() + 1;
            if (isEmpty(data)) {
                return;
            }
            rows.add(new ImportRow(rowNumber, data));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // no-op
        }

        private boolean isEmpty(DeviceImportExcelRow row) {
            return StringUtils.isAllBlank(
                    row.getDeviceName(),
                    row.getDeviceType(),
                    row.getVendor(),
                    row.getDescription()
            );
        }

        private List<ImportRow> rows() {
            return rows;
        }
    }

    private static final class ImportTaskContext {

        private final String taskId;
        private final String operator;
        private final long startedAt;

        private volatile String status;
        private volatile String stage;
        private volatile int totalRows;
        private volatile int processedRows;
        private volatile int successCount;
        private volatile int failedCount;
        private volatile int progressPercent;
        private volatile String message;
        private volatile Long finishedAt;
        private volatile DeviceImportResultVO result;

        private ImportTaskContext(String taskId, String operator) {
            this.taskId = taskId;
            this.operator = operator;
            this.startedAt = System.currentTimeMillis();
            this.status = "PENDING";
            this.stage = "QUEUED";
            this.totalRows = 0;
            this.processedRows = 0;
            this.successCount = 0;
            this.failedCount = 0;
            this.progressPercent = 0;
            this.message = "Queued";
            this.finishedAt = null;
            this.result = null;
        }

        static ImportTaskContext pending(String taskId, String operator) {
            return new ImportTaskContext(taskId, operator);
        }

        synchronized void markRunning(String stage, String message) {
            this.status = "RUNNING";
            this.stage = stage;
            this.message = message;
            this.finishedAt = null;
            if (this.totalRows <= 0) {
                this.progressPercent = 0;
            }
        }

        synchronized void setTotalRows(int totalRows) {
            this.totalRows = Math.max(totalRows, 0);
            this.processedRows = Math.min(this.processedRows, this.totalRows);
            this.progressPercent = calculateProgressPercent(this.status, this.totalRows, this.processedRows);
        }

        synchronized void updateProgress(int totalRows, int processedRows, int successCount, int failedCount) {
            this.totalRows = Math.max(totalRows, 0);
            this.processedRows = Math.max(processedRows, 0);
            this.successCount = Math.max(successCount, 0);
            this.failedCount = Math.max(failedCount, 0);
            this.progressPercent = calculateProgressPercent(this.status, this.totalRows, this.processedRows);
        }

        synchronized void markSuccess(DeviceImportResultVO result, String message) {
            this.status = "SUCCESS";
            this.stage = "DONE";
            this.result = result;
            this.totalRows = result.getTotalRows() == null ? 0 : result.getTotalRows();
            this.processedRows = this.totalRows;
            this.successCount = result.getSuccessCount() == null ? 0 : result.getSuccessCount();
            this.failedCount = result.getFailedCount() == null ? 0 : result.getFailedCount();
            this.progressPercent = 100;
            this.message = message;
            this.finishedAt = System.currentTimeMillis();
        }

        synchronized void markFailed(String message, DeviceImportResultVO result) {
            this.status = "FAILED";
            this.stage = "DONE";
            this.result = result;
            this.progressPercent = this.totalRows > 0 && this.processedRows >= this.totalRows ? 100 : this.progressPercent;
            this.message = message;
            this.finishedAt = System.currentTimeMillis();
        }

        synchronized DeviceImportTaskVO toVO() {
            return DeviceImportTaskVO.builder()
                    .taskId(taskId)
                    .status(status)
                    .stage(stage)
                    .totalRows(totalRows)
                    .processedRows(processedRows)
                    .successCount(successCount)
                    .failedCount(failedCount)
                    .progressPercent(progressPercent)
                    .message(message)
                    .startedAt(startedAt)
                    .finishedAt(finishedAt)
                    .result(result)
                    .build();
        }

        boolean isExpired(long nowMillis) {
            Long finished = this.finishedAt;
            if (finished == null) {
                return false;
            }
            return nowMillis - finished > TASK_EXPIRE_MILLIS;
        }

        String getOperator() {
            return operator;
        }

        private int calculateProgressPercent(String status, int totalRows, int processedRows) {
            if ("SUCCESS".equals(status)) {
                return 100;
            }
            if (totalRows <= 0) {
                return 0;
            }
            int value = (int) Math.floor((processedRows * 100.0d) / totalRows);
            if ("RUNNING".equals(status)) {
                return Math.min(value, 99);
            }
            return Math.min(value, 100);
        }
    }
}
