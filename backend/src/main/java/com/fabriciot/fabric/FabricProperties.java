package com.fabriciot.fabric;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.fabric")
public class FabricProperties {

    private boolean enabled = true;
    private boolean mock = true;
    private String mspId;
    private String channelName;
    private String chaincodeName;
    private String contractName;
    private String peerEndpoint;
    private boolean tlsEnabled;
    private String certPath;
    private String keyPath;
    private String tlsCertPath;
    private String hostnameOverride;

    public String getContractName() {
        return StringUtils.defaultIfBlank(contractName, chaincodeName);
    }
}

