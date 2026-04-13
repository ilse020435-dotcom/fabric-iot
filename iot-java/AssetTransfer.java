package org.hyperledger.fabric.samples.assettransfer;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(name = "DeviceContract")
@Default
public class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transaction()
    public void CreateDevice(final Context ctx, String deviceId, String name, String owner, String operator, String operateTime) {
        String existing = ctx.getStub().getStringState(deviceId);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("Device already exists: " + deviceId);
        }

        String normalizedOperator = (operator == null || operator.isBlank()) ? owner : operator;
        String normalizedOperateTime = normalizeOperateTime(operateTime);
        Device device = new Device(deviceId, name, owner, "OFFLINE", normalizedOperator, normalizedOperateTime);
        ctx.getStub().putStringState(deviceId, genson.serialize(device));
    }

    @Transaction()
    public String ReadDevice(final Context ctx, String deviceId) {
        String json = ctx.getStub().getStringState(deviceId);
        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("Device not found: " + deviceId);
        }

        return json;
    }

    @Transaction()
    public String UpdateStatus(final Context ctx, String deviceId, String status, String operator, String operateTime) {
        String json = ReadDevice(ctx, deviceId);
        Device device = genson.deserialize(json, Device.class);
        String normalizedOperator = (operator == null || operator.isBlank()) ? device.getOperator() : operator;
        String normalizedOperateTime = normalizeOperateTime(operateTime);

        Device newDevice = new Device(
                device.getDeviceId(),
                device.getName(),
                device.getOwner(),
                status,
                normalizedOperator,
                normalizedOperateTime
        );

        String newJson = genson.serialize(newDevice);
        ctx.getStub().putStringState(deviceId, newJson);

        return newJson;
    }

    private String normalizeOperateTime(String operateTime) {
        if (operateTime == null || operateTime.isBlank()) {
            return LocalDateTime.now().format(TIME_FORMATTER);
        }
        return operateTime;
    }

    @Transaction()
    public String GetAllDevices(final Context ctx) {
        List<Device> list = new ArrayList<>();

        QueryResultsIterator<KeyValue> results = ctx.getStub().getStateByRange("", "");

        for (KeyValue kv : results) {
            Device device = genson.deserialize(kv.getStringValue(), Device.class);
            list.add(device);
        }

        return genson.serialize(list);
    }
}
