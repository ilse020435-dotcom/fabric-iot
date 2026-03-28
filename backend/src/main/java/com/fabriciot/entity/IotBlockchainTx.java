package com.fabriciot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "iot_blockchain_tx", autoResultMap = true)
public class IotBlockchainTx {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tx_hash")
    private String txHash;

    @TableField("block_height")
    private Long blockHeight;

    @TableField("device_id")
    private String deviceId;

    @TableField("operation_type")
    private String operationType;

    @TableField("channel_name")
    private String channelName;

    @TableField("contract_name")
    private String contractName;

    @TableField("summary_hash")
    private String summaryHash;

    @TableField("chain_status")
    private String chainStatus;

    @TableField(value = "payload_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> payloadJson;

    @TableField(value = "write_set_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> writeSetJson;

    @TableLogic
    private Integer deleted;

    private LocalDateTime timestamp;
}

