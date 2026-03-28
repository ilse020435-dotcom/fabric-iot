package com.fabriciot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fabriciot.entity.IotDeviceStatusSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IotDeviceStatusSnapshotMapper extends BaseMapper<IotDeviceStatusSnapshot> {

    @Select("""
            SELECT *
            FROM iot_device_status_snapshot
            WHERE device_id = #{deviceId}
              AND deleted = 0
            ORDER BY updated_at DESC
            LIMIT 1
            """)
    IotDeviceStatusSnapshot selectLatestByDeviceId(@Param("deviceId") String deviceId);

    @Select("""
            SELECT *
            FROM iot_device_status_snapshot
            WHERE device_id = #{deviceId}
              AND deleted = 0
            ORDER BY updated_at DESC
            LIMIT #{limit}
            """)
    List<IotDeviceStatusSnapshot> selectRecentByDeviceId(@Param("deviceId") String deviceId, @Param("limit") Integer limit);
}

