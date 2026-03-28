package com.fabriciot.dto.device;

import com.fabriciot.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceListQuery extends PageQuery {

    private String deviceId;
    private String deviceName;
    private String status;
}

