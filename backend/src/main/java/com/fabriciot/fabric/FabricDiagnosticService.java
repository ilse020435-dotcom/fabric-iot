package com.fabriciot.fabric;

import com.fabriciot.vo.fabric.FabricTestVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class FabricDiagnosticService {

    private static final int TCP_TIMEOUT_MILLIS = (int) Duration.ofSeconds(3).toMillis();

    private final FabricProperties properties;
    private final ObjectProvider<GatewayFabricGatewayClient> gatewayClientProvider;

    public FabricDiagnosticService(FabricProperties properties,
                                   ObjectProvider<GatewayFabricGatewayClient> gatewayClientProvider) {
        this.properties = properties;
        this.gatewayClientProvider = gatewayClientProvider;
    }

    public FabricTestVO testConnection(String function, List<String> args) {
        List<String> safeArgs = args == null ? List.of() : args.stream()
                .filter(StringUtils::isNotBlank)
                .toList();
        List<String> issues = new ArrayList<>();

        boolean certConfigured = StringUtils.isNotBlank(properties.getCertPath());
        boolean keyConfigured = StringUtils.isNotBlank(properties.getKeyPath());
        boolean tlsCertConfigured = StringUtils.isNotBlank(properties.getTlsCertPath());
        boolean gatewayConfigReady = properties.isEnabled()
                && StringUtils.isNoneBlank(properties.getPeerEndpoint(), properties.getMspId(),
                properties.getChannelName(), properties.getChaincodeName())
                && certConfigured
                && keyConfigured
                && (!properties.isTlsEnabled() || tlsCertConfigured);

        if (!properties.isEnabled()) {
            issues.add("app.fabric.enabled=false");
        }
        if (properties.isMock()) {
            issues.add("app.fabric.mock=true, current backend uses MockFabricGatewayClient");
        }
        if (!certConfigured) {
            issues.add("certPath is blank");
        }
        if (!keyConfigured) {
            issues.add("keyPath is blank");
        }
        if (properties.isTlsEnabled() && !tlsCertConfigured) {
            issues.add("tlsCertPath is blank while TLS is enabled");
        }

        boolean tcpReachable = testPeerReachability(properties.getPeerEndpoint(), issues);
        GatewayFabricGatewayClient gatewayClient = gatewayClientProvider.getIfAvailable();
        boolean gatewayReady = gatewayClient != null;
        if (!properties.isMock() && properties.isEnabled() && !gatewayReady) {
            issues.add("GatewayFabricGatewayClient is not available");
        }

        boolean invocationRequested = StringUtils.isNotBlank(function);
        boolean invocationSuccess = false;
        String evaluateResult = null;
        String error = null;

        if (invocationRequested) {
            if (!properties.isEnabled()) {
                issues.add("Chaincode invocation skipped because Fabric is disabled");
            } else if (properties.isMock()) {
                issues.add("Chaincode invocation skipped because backend is running in mock mode");
            } else if (!gatewayReady) {
                issues.add("Chaincode invocation skipped because real gateway is not ready");
            } else {
                try {
                    evaluateResult = gatewayClient.evaluateTransaction(function, safeArgs.toArray(String[]::new));
                    invocationSuccess = true;
                } catch (Exception ex) {
                    error = ex.getMessage();
                    issues.add("Chaincode invocation failed: " + ex.getMessage());
                }
            }
        }

        boolean basicConnectionSuccess = properties.isEnabled()
                && !properties.isMock()
                && gatewayConfigReady
                && tcpReachable
                && gatewayReady;
        boolean endToEndSuccess = basicConnectionSuccess && (!invocationRequested || invocationSuccess);
        String message = buildMessage(properties.isEnabled(), properties.isMock(), basicConnectionSuccess,
                invocationRequested, invocationSuccess);

        return FabricTestVO.builder()
                .enabled(properties.isEnabled())
                .mock(properties.isMock())
                .tlsEnabled(properties.isTlsEnabled())
                .certConfigured(certConfigured)
                .keyConfigured(keyConfigured)
                .tlsCertConfigured(tlsCertConfigured)
                .gatewayConfigReady(gatewayConfigReady)
                .tcpReachable(tcpReachable)
                .gatewayReady(gatewayReady)
                .chaincodeInvocationRequested(invocationRequested)
                .chaincodeInvocationSuccess(invocationSuccess)
                .basicConnectionSuccess(basicConnectionSuccess)
                .endToEndSuccess(endToEndSuccess)
                .success(invocationRequested ? endToEndSuccess : basicConnectionSuccess)
                .peerEndpoint(properties.getPeerEndpoint())
                .mspId(properties.getMspId())
                .channelName(properties.getChannelName())
                .chaincodeName(properties.getChaincodeName())
                .contractName(properties.getContractName())
                .function(function)
                .args(safeArgs)
                .evaluateResult(evaluateResult)
                .message(message)
                .error(error)
                .issues(issues)
                .build();
    }

    private boolean testPeerReachability(String peerEndpoint, List<String> issues) {
        Endpoint endpoint = parseEndpoint(peerEndpoint);
        if (endpoint == null) {
            issues.add("peerEndpoint is blank or unsupported: " + peerEndpoint);
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()), TCP_TIMEOUT_MILLIS);
            return true;
        } catch (IOException ex) {
            issues.add("Peer TCP check failed: " + ex.getMessage());
            return false;
        }
    }

    private Endpoint parseEndpoint(String peerEndpoint) {
        if (StringUtils.isBlank(peerEndpoint)) {
            return null;
        }

        String trimmed = peerEndpoint.trim();
        try {
            if (trimmed.contains("://")) {
                URI uri = URI.create(trimmed);
                if (StringUtils.isNotBlank(uri.getHost()) && uri.getPort() > 0) {
                    return new Endpoint(uri.getHost(), uri.getPort());
                }
                String path = uri.getPath();
                if (StringUtils.isNotBlank(path)) {
                    String candidate = path.startsWith("/") ? path.substring(1) : path;
                    return parseHostPort(candidate);
                }
                return null;
            }
            return parseHostPort(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Endpoint parseHostPort(String value) {
        int index = value.lastIndexOf(':');
        if (index < 1 || index == value.length() - 1) {
            return null;
        }
        String host = value.substring(0, index).trim();
        String portText = value.substring(index + 1).trim();
        if (StringUtils.isAnyBlank(host, portText)) {
            return null;
        }
        int port = Integer.parseInt(portText);
        if (port < 1 || port > 65535) {
            return null;
        }
        return new Endpoint(host, port);
    }

    private String buildMessage(boolean enabled, boolean mock, boolean basicConnectionSuccess,
                                boolean invocationRequested, boolean invocationSuccess) {
        if (!enabled) {
            return "Fabric is disabled by configuration";
        }
        if (mock) {
            return "Fabric test is running against mock mode, not a real Fabric network";
        }
        if (!basicConnectionSuccess) {
            return "Basic Fabric connectivity check failed";
        }
        if (!invocationRequested) {
            return "Basic Fabric connectivity is ready, chaincode invocation was not executed";
        }
        return invocationSuccess
                ? "Fabric end-to-end test succeeded"
                : "Fabric basic connectivity is ready, but chaincode invocation failed";
    }

    private record Endpoint(String host, int port) {
    }
}
