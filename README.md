# 基于区块链的物联网设备生命周期管理系统（前后端分离骨架）

## 目录结构

```text
Fabric-IOT/
├── frontend/                     # Vue3 + Vite 前端
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── layout/
│   │   ├── router/
│   │   ├── store/
│   │   ├── views/
│   │   │   ├── dashboard/
│   │   │   ├── device/
│   │   │   ├── monitor/
│   │   │   ├── auth/
│   │   │   ├── audit/
│   │   │   └── blockchain/
│   │   ├── mock/
│   │   ├── App.vue
│   │   └── main.js
│   ├── package.json
│   └── vite.config.js
└── backend/                      # Spring Boot 3 后端
    ├── src/main/java/com/example/iot/
    │   ├── config/
    │   ├── controller/
    │   ├── service/
    │   ├── service/impl/
    │   ├── mapper/
    │   ├── entity/
    │   ├── dto/
    │   ├── vo/
    │   ├── utils/
    │   ├── common/
    │   └── Application.java
    ├── src/main/resources/application.yml
    ├── sql/schema.sql
    └── pom.xml
```

## 技术栈

- 前端：Vue 3 + Vite + Vue Router + Pinia + Element Plus + Axios + ECharts
- 后端：Java 17 + Spring Boot 3 + Spring MVC + Spring Security + JWT + MyBatis-Plus + MySQL + Lombok

## 前端说明

- 默认开启 mock（`frontend/.env.example` 中 `VITE_USE_MOCK=true`）
- 所有 API 统一走 `/api/*`，与后端控制器路径对齐
- 页面包含：
  - Dashboard
  - Device
  - Monitor
  - Auth
  - Audit
  - Blockchain

## 后端说明

- JWT 登录接口：`POST /api/auth/login`
- 核心接口：
  - `GET /api/device`
  - `GET /api/device/{id}`
  - `POST /api/device`
  - `POST /api/device/freeze`
  - `POST /api/device/revoke`
  - `GET /api/status`
  - `GET /api/audit`
- Fabric 采用模拟实现：`FabricServiceMockImpl`
  - `registerDevice()`
  - `queryDevice()`
  - `recordStatusHash()`

## 数据库

- SQL 文件：`backend/sql/schema.sql`
- 表包含：
  - `user`
  - `role`
  - `permission`
  - `device`
  - `device_status`
  - `audit_log`
  - `blockchain_tx`

## 启动步骤（手动）

1. 创建 MySQL 数据库：`fabric_iot`
2. 执行 `backend/sql/schema.sql`
3. 按需修改 `backend/src/main/resources/application.yml` 数据库配置
4. 分别启动前后端

> 当前仅提供可运行基础骨架与示例业务代码，便于你继续扩展真实 Fabric SDK、RBAC 细粒度鉴权、分页与审计追踪。
