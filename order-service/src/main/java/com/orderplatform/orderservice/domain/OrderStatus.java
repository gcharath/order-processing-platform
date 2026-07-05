package com.orderplatform.orderservice.domain;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLING,
    CANCELLED,
    FULFILLING,
    FULFILLED,
    FAILED
}
