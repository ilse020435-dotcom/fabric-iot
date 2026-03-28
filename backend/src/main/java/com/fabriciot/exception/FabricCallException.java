package com.fabriciot.exception;

public class FabricCallException extends BizException {

    public FabricCallException(String message) {
        super(ErrorCode.FABRIC_ERROR, message);
    }

    public FabricCallException(String message, Throwable cause) {
        super(ErrorCode.FABRIC_ERROR, message + " - " + cause.getMessage());
    }
}

