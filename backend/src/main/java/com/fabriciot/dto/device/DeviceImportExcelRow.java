package com.fabriciot.dto.device;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DeviceImportExcelRow {

    @ExcelProperty(value = "设备名称", index = 0)
    private String deviceName;

    @ExcelProperty(value = "设备类型", index = 1)
    private String deviceType;

    @ExcelProperty(value = "厂商", index = 2)
    private String vendor;

    @ExcelProperty(value = "描述", index = 3)
    private String description;
}
