package org.hyperledger.fabric.samples.assettransfer;

import java.util.ArrayList;
import java.util.List;

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

    @Transaction()
    public void CreateDevice(final Context ctx, String deviceId, String name, String owner) {
        String existing = ctx.getStub().getStringState(deviceId);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("Device already exists: " + deviceId);
        }

        Device device = new Device(deviceId, name, owner, "OFFLINE");
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
    public String UpdateStatus(final Context ctx, String deviceId, String status) {
        String json = ReadDevice(ctx, deviceId);
        Device device = genson.deserialize(json, Device.class);

        Device newDevice = new Device(
                device.getDeviceId(),
                device.getName(),
                device.getOwner(),
                status
        );

        String newJson = genson.serialize(newDevice);
        ctx.getStub().putStringState(deviceId, newJson);

        return newJson;
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