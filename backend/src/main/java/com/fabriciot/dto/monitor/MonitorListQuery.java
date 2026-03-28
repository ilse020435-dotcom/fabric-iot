package com.fabriciot.dto.monitor;

import com.fabriciot.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MonitorListQuery extends PageQuery {

    private String deviceId;
    private String status;
}

