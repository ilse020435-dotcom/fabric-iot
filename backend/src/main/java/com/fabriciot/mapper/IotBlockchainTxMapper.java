package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fabriciot.entity.IotBlockchainTx;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IotBlockchainTxMapper extends BaseMapper<IotBlockchainTx> {

    @Results(id = "blockchainTxResultMap", value = {
            @Result(column = "payload_json", property = "payloadJson", typeHandler = JacksonTypeHandler.class),
            @Result(column = "write_set_json", property = "writeSetJson", typeHandler = JacksonTypeHandler.class)
    })
    @Select("""
            SELECT *
            FROM iot_blockchain_tx
            WHERE tx_hash = #{txHash}
              AND deleted = 0
            LIMIT 1
            """)
    IotBlockchainTx selectByTxHash(@Param("txHash") String txHash);
}

