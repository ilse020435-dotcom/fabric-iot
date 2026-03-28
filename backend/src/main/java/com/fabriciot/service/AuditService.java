package com.fabriciot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fabriciot.dto.audit.AuditListQuery;
import com.fabriciot.vo.audit.AuditLogVO;

public interface AuditService {

    IPage<AuditLogVO> list(AuditListQuery query);

    AuditLogVO detail(String logId);
}

