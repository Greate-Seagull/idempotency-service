package com.hungvers.idempotency.starter.logger;

import com.hungvers.idempotency.api.service.port.Logger;

public class Slf4jLogger implements Logger {
    private final org.slf4j.Logger logger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    public Slf4jLogger(Class<?> clazz) {
        this.logger = org.slf4j.LoggerFactory.getLogger(clazz);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        this.logger.warn(message, throwable);
    }
}
