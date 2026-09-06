package com.training.oms.order.exception;

/** Raised from Resilience4j fallback methods when a downstream call ultimately fails. */
public class DownstreamServiceException extends RuntimeException {
    public DownstreamServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
