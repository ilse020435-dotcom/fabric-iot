# 查询全部
peer chaincode query -C mychannel -n iotjava -c '{"Args":["GetAllDevices"]}'
# 查询单个
peer chaincode query -C mychannel -n iotjava -c '{"Args":["ReadDevice","dev001"]}'

# 添加设备
peer chaincode invoke -o localhost:7050 \ 
--ordererTLSHostnameOverride orderer.example.com \ 
--tls \ 
--cafile "${PWD}/organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem" \ 
-C mychannel -n iotjava \ --peerAddresses localhost:7051 \ 
--tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem" \ 
--peerAddresses localhost:9051 \ 
--tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem" \ 
-c '{"Args":["CreateDevice","dev001","sensor1","Alice"]}'

# 修改设备状态
peer chaincode invoke -o localhost:7050 \
--ordererTLSHostnameOverride orderer.example.com \
--tls \
--cafile "${PWD}/organizations/ordererOrganizations/example.com/tlsca/tlsca.example.com-cert.pem" \
-C mychannel -n iotjava \
--peerAddresses localhost:7051 \
--tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem" \
--peerAddresses localhost:9051 \
--tlsRootCertFiles "${PWD}/organizations/peerOrganizations/org2.example.com/tlsca/tlsca.org2.example.com-cert.pem" \
-c '{"Args":["UpdateStatus","dev001","ONLINE"]}'