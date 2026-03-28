# Windows 上用 Docker + WSL2 启动 Fabric 测试网络（单机）

本指南适用于 Windows 10/11，本地启动 Hyperledger Fabric `test-network`，并部署 Java 链码用于联调。

## 1. 前置条件

请先确认以下环境：

1. 已启用 CPU 虚拟化（BIOS/UEFI）。
2. 已安装 WSL2（建议 Ubuntu 22.04 或 24.04）。
3. 已安装 Docker Desktop，并使用 WSL2 引擎。
4. 已安装 Git（建议带 Git Bash，或直接使用 WSL 终端）。

建议主要在 WSL 终端中执行 Fabric 命令，避免 Windows 路径和换行符问题。

## 2. 安装并检查 WSL2

在 PowerShell（管理员）执行：

```powershell
wsl --install -d Ubuntu-22.04
```

安装完成后重启系统，再检查：

```powershell
wsl -l -v
```

确认目标发行版是 `VERSION 2`。若不是，可执行：

```powershell
wsl --set-version Ubuntu-22.04 2
```

## 3. 配置 Docker Desktop

打开 Docker Desktop，确认以下设置：

1. `Settings -> General -> Use the WSL 2 based engine` 已勾选。
2. `Settings -> Resources -> WSL Integration` 中已启用你的 Ubuntu 发行版。

进入 WSL 终端后检查：

```bash
docker --version
docker compose version
```

若两条命令都能返回版本号，说明 Docker 与 WSL 集成正常。

## 4. 下载 Fabric Samples、二进制和镜像

在 WSL 终端执行：

```bash
sudo apt update
sudo apt install -y curl git jq

mkdir -p ~/fabric
cd ~/fabric

curl -sSLO https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh
chmod +x install-fabric.sh
./install-fabric.sh docker binary samples
```

执行完成后，你会看到目录结构：

```text
~/fabric/fabric-samples/
├── bin/
├── config/
└── test-network/
```

## 5. 启动单机测试网络

```bash
cd ~/fabric/fabric-samples/test-network
./network.sh up createChannel -c mychannel -ca
```

这条命令会完成：

1. 启动 orderer、peer、CA 容器。
2. 创建并加入通道 `mychannel`。

## 6. 验证网络是否启动成功

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

正常应看到类似容器：

- `orderer.example.com`
- `peer0.org1.example.com`
- `peer0.org2.example.com`
- `ca_org1`
- `ca_org2`

## 7. 部署 Java 链码（官方示例）

在 `test-network` 目录执行：

```bash
./network.sh deployCC \
  -ccn basic \
  -ccp ../asset-transfer-basic/chaincode-java \
  -ccl java
```

若命令成功，说明当前环境已具备 Fabric + Java 链码部署能力。

## 8. 停止并清理网络

```bash
cd ~/fabric/fabric-samples/test-network
./network.sh down
```

## 9. 常见问题

1. `Cannot connect to the Docker daemon`
   检查 Docker Desktop 是否已启动，以及 WSL Integration 是否开启。

2. `./network.sh: Permission denied`
   执行 `chmod +x network.sh` 后重试。

3. 端口冲突（如 `7051/7054/7050`）
   先执行 `./network.sh down`，必要时关闭占用端口的进程后再启动。

4. 路径/换行问题（Windows 目录、CRLF）
   优先在 WSL 的 Linux 路径中操作（例如 `~/fabric`），避免在 `C:\` 路径直接运行 Fabric 脚本。

---

截至 `2026-03-24`，本指南基于官方 `install-fabric.sh` 安装流程整理。若后续脚本参数变更，请以 Hyperledger Fabric 官方文档为准。
## 10.重启电脑后启动服务

wsl
cd ~/fabric-samples/test-network
./network.sh up
./network.sh deployCC -ccn basic -ccp ../asset-transfer-basic/chaincode-java -ccl java
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
export CORE_PEER_ADDRESS=localhost:7051
peer chaincode query -C mychannel -n basic -c '{"Args":["GetAllAssets"]}'





cd ~/fabric-samples/test-network
./network.sh up
export PATH=${PWD}/../bin:$PATH
export FABRIC_CFG_PATH=${PWD}/../config/
export CORE_PEER_TLS_ENABLED=true
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/tlsca/tlsca.org1.example.com-cert.pem
export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp
export CORE_PEER_ADDRESS=localhost:7051
peer chaincode query -C mychannel -n iotjava -c '{"Args":["GetAllDevices"]}'