package com.fabriciot.exception;

public enum ErrorCode {
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证或 token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BIZ_ERROR(422, "业务处理失败"),
    FABRIC_ERROR(520, "Fabric 调用失败"),
    DATABASE_ERROR(530, "数据库操作失败"),
    SYSTEM_ERROR(500, "系统内部错误");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
