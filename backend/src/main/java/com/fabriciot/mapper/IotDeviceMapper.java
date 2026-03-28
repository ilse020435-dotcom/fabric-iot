package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fabriciot.entity.IotDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IotDeviceMapper extends BaseMapper<IotDevice> {

    @Select("""
            SELECT *
            FROM iot_device
            WHERE device_id = #{deviceId}
              AND deleted = 0
            LIMIT 1
            """)
    IotDevice selectByDeviceId(@Param("deviceId") String deviceId);

    @Select("""
            SELECT *
            FROM iot_device
            WHERE did = #{did}
              AND deleted = 0
            LIMIT 1
            """)
    IotDevice selectByDid(@Param("did") String did);
}

