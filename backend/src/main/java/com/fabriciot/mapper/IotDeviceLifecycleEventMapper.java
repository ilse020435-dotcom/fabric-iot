package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fabriciot.entity.IotDeviceLifecycleEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IotDeviceLifecycleEventMapper extends BaseMapper<IotDeviceLifecycleEvent> {

    @Select("""
            SELECT *
            FROM iot_device_lifecycle_event
            WHERE device_id = #{deviceId}
              AND deleted = 0
            ORDER BY occurred_at DESC
            LIMIT #{limit}
            """)
    List<IotDeviceLifecycleEvent> selectRecentByDeviceId(@Param("deviceId") String deviceId, @Param("limit") Integer limit);
}

