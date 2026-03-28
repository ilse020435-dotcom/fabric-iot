package com.fabriciot.dto.audit;

import com.fabriciot.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditListQuery extends PageQuery {

    private String startTime;
    private String endTime;
    private String operationType;
    private String deviceId;
    private String operator;
}

