package org.hyperledger.fabric.samples.assettransfer;

import com.owlike.genson.annotation.JsonProperty;

public final class Device {

    private final String deviceId;
    private final String name;
    private final String owner;
    private final String status;

    public Device(
            @JsonProperty("deviceId") final String deviceId,
            @JsonProperty("name") final String name,
            @JsonProperty("owner") final String owner,
            @JsonProperty("status") final String status) {
        this.deviceId = deviceId;
        this.name = name;
        this.owner = owner;
        this.status = status;
    }

    public String getDeviceId() { return deviceId; }
    public String getName() { return name; }
    public String getOwner() { return owner; }
    public String getStatus() { return status; }
}