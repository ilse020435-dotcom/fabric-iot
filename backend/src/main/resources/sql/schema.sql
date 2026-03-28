CREATE TABLE IF NOT EXISTS sys_user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(16)  NOT NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code  VARCHAR(64)  NOT NULL UNIQUE,
    role_name  VARCHAR(64)  NOT NULL,
    role_desc  VARCHAR(255) NULL,
    deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    perm_code VARCHAR(64) NOT NULL UNIQUE,
    perm_name VARCHAR(64) NOT NULL,
    parent_id BIGINT NULL DEFAULT 0,
    type      VARCHAR(16) NOT NULL,
    deleted   TINYINT(1)  NOT NULL DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role
(
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS iot_device
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id        VARCHAR(64)  NOT NULL UNIQUE,
    device_name      VARCHAR(128) NOT NULL,
    did              VARCHAR(128) NOT NULL UNIQUE,
    device_type      VARCHAR(64)  NOT NULL,
    vendor           VARCHAR(128) NOT NULL,
    lifecycle_status VARCHAR(16)  NOT NULL,
    description      VARCHAR(500) NULL,
    deleted          TINYINT(1)   NOT NULL DEFAULT 0,
    register_time    DATETIME     NOT NULL,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL,
    INDEX idx_device_lifecycle_status (lifecycle_status),
    INDEX idx_device_vendor (vendor),
    INDEX idx_device_deleted (deleted, updated_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS iot_device_lifecycle_event
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id      VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    before_status  VARCHAR(16) NULL,
    after_status   VARCHAR(16) NOT NULL,
    summary_hash   VARCHAR(66) NULL,
    operator       VARCHAR(64) NOT NULL,
    occurred_at    DATETIME    NOT NULL,
    tx_hash        VARCHAR(66) NULL,
    deleted        TINYINT(1)  NOT NULL DEFAULT 0,
    INDEX idx_lifecycle_device_time (device_id, occurred_at DESC),
    INDEX idx_lifecycle_tx_hash (tx_hash),
    INDEX idx_lifecycle_deleted (deleted, occurred_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS iot_device_status_snapshot
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id       VARCHAR(64) NOT NULL,
    monitor_status  VARCHAR(16) NOT NULL,
    signal_strength VARCHAR(16) NULL,
    temperature     VARCHAR(16) NULL,
    summary_hash    VARCHAR(66) NOT NULL,
    block_height    BIGINT NULL,
    tx_hash         VARCHAR(66) NULL,
    deleted         TINYINT(1)  NOT NULL DEFAULT 0,
    updated_at      DATETIME    NOT NULL,
    INDEX idx_snapshot_device_time (device_id, updated_at DESC),
    INDEX idx_snapshot_status (monitor_status),
    INDEX idx_snapshot_deleted (deleted, updated_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS iot_audit_log
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id         VARCHAR(64) NOT NULL UNIQUE,
    operation_time DATETIME    NOT NULL,
    operator       VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    device_id      VARCHAR(64) NOT NULL,
    on_chain       TINYINT(1)  NOT NULL,
    tx_hash        VARCHAR(66) NULL,
    remark         VARCHAR(255) NULL,
    detail_json    JSON NULL,
    deleted        TINYINT(1)  NOT NULL DEFAULT 0,
    created_at     DATETIME    NOT NULL,
    CONSTRAINT chk_audit_on_chain_tx CHECK (on_chain = 0 OR tx_hash IS NOT NULL),
    INDEX idx_audit_time (operation_time DESC),
    INDEX idx_audit_device (device_id),
    INDEX idx_audit_operator (operator),
    INDEX idx_audit_type (operation_type),
    INDEX idx_audit_tx_hash (tx_hash),
    INDEX idx_audit_deleted (deleted, operation_time DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS iot_blockchain_tx
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    tx_hash       VARCHAR(66)  NOT NULL UNIQUE,
    block_height  BIGINT       NOT NULL,
    device_id     VARCHAR(64)  NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    channel_name  VARCHAR(64)  NOT NULL,
    contract_name VARCHAR(128) NOT NULL,
    summary_hash  VARCHAR(66)  NOT NULL,
    chain_status  VARCHAR(16)  NOT NULL,
    payload_json  JSON NULL,
    write_set_json JSON NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    timestamp     DATETIME     NOT NULL,
    INDEX idx_chain_block (block_height DESC),
    INDEX idx_chain_device (device_id),
    INDEX idx_chain_status (chain_status),
    INDEX idx_chain_deleted (deleted, timestamp DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
