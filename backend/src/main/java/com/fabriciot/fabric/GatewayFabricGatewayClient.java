package com.fabriciot.fabric;

import com.fabriciot.exception.FabricCallException;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Proposal;
import org.hyperledger.fabric.client.Status;
import org.hyperledger.fabric.client.SubmittedTransaction;
import org.hyperledger.fabric.client.Transaction;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.fabric", name = "mock", havingValue = "false")
public class GatewayFabricGatewayClient implements FabricGatewayClient, DisposableBean {

    private final FabricProperties properties;
    private ManagedChannel channel;
    private Gateway gateway;

    public GatewayFabricGatewayClient(FabricProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.warn("Fabric is disabled by configuration");
            return;
        }
        if (StringUtils.isAnyBlank(properties.getCertPath(), properties.getKeyPath(), properties.getPeerEndpoint(),
                properties.getMspId(), properties.getChannelName(), properties.getChaincodeName())) {
            throw new IllegalStateException("Fabric gateway config is incomplete");
        }
        try (Reader certReader = Files.newBufferedReader(Path.of(properties.getCertPath()));
             Reader keyReader = Files.newBufferedReader(Path.of(properties.getKeyPath()))) {
            X509Certificate certificate = Identities.readX509Certificate(certReader);
            PrivateKey privateKey = Identities.readPrivateKey(keyReader);
            Identity identity = new X509Identity(properties.getMspId(), certificate);
            Signer signer = Signers.newPrivateKeySigner(privateKey);

            NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget(properties.getPeerEndpoint());
            if (properties.isTlsEnabled()) {
                if (StringUtils.isBlank(properties.getTlsCertPath())) {
                    throw new IllegalStateException("TLS enabled but tlsCertPath is blank");
                }
                SslContext sslContext = GrpcSslContexts.forClient()
                        .trustManager(Path.of(properties.getTlsCertPath()).toFile())
                        .build();
                channelBuilder.sslContext(sslContext);
                if (StringUtils.isNotBlank(properties.getHostnameOverride())) {
                    channelBuilder.overrideAuthority(properties.getHostnameOverride());
                }
            } else {
                channelBuilder.usePlaintext();
            }
            this.channel = channelBuilder.build();
            this.gateway = Gateway.newInstance()
                    .identity(identity)
                    .signer(signer)
                    .connection(channel)
                    .connect();
            log.info("Fabric gateway initialized, channel={}, chaincode={}, contract={}",
                    properties.getChannelName(), properties.getChaincodeName(), properties.getContractName());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Fabric gateway", e);
        }
    }

    @Override
    public FabricTransactionResult submitTransaction(String function, String... args) {
        try {
            Contract contract = getContract();
            Proposal proposal = contract.newProposal(function).addArguments(args).build();
            Transaction transaction = proposal.endorse();
            SubmittedTransaction submittedTransaction = transaction.submitAsync();
            Status status = submittedTransaction.getStatus();
            Map<String, Object> writeSet = new HashMap<>();
            writeSet.put("transactionId", transaction.getTransactionId());
            writeSet.put("validationCode", status.getCode().name());
            return FabricTransactionResult.builder()
                    .success(status.isSuccessful())
                    .txHash(transaction.getTransactionId())
                    .blockHeight(status.getBlockNumber())
                    .responsePayload(new String(submittedTransaction.getResult(), StandardCharsets.UTF_8))
                    .writeSet(writeSet)
                    .message(status.isSuccessful() ? "success" : "failed")
                    .build();
        } catch (Exception e) {
            throw new FabricCallException("Fabric submitTransaction failed", e);
        }
    }

    @Override
    public String evaluateTransaction(String function, String... args) {
        try {
            Contract contract = getContract();
            byte[] bytes = contract.evaluateTransaction(function, args);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new FabricCallException("Fabric evaluateTransaction failed", e);
        }
    }

    private Contract getContract() {
        if (gateway == null) {
            throw new FabricCallException("Fabric gateway is not initialized");
        }
        if (StringUtils.isBlank(properties.getContractName())) {
            return gateway.getNetwork(properties.getChannelName()).getContract(properties.getChaincodeName());
        }
        return gateway.getNetwork(properties.getChannelName()).getContract(properties.getChaincodeName(), properties.getContractName());
    }

    @Override
    public void destroy() {
        if (gateway != null) {
            gateway.close();
        }
        if (channel != null) {
            channel.shutdownNow();
        }
    }
}

