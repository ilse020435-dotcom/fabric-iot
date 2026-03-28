package com.fabriciot.service;

import com.fabriciot.vo.device.DeviceImportResultVO;
import com.fabriciot.vo.device.DeviceImportTaskVO;
import org.springframework.web.multipart.MultipartFile;

public interface DeviceImportService {

    DeviceImportResultVO importExcel(MultipartFile file, String operator);

    DeviceImportTaskVO startImportTask(MultipartFile file, String operator);

    DeviceImportTaskVO queryImportTask(String taskId, String operator);

    byte[] exportTemplate();
}
