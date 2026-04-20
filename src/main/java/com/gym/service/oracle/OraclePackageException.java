package com.gym.service.oracle;

public class OraclePackageException extends RuntimeException {

    public OraclePackageException(String message) {
        super(message);
    }

    public OraclePackageException(String message, Throwable cause) {
        super(message, cause);
    }
}