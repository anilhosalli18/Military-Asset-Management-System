package com.mams.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransferStatus {
    pending("pending"),
    in_transit("in_transit"),
    completed("completed"),
    cancelled("cancelled");

    private final String value;

    TransferStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransferStatus fromValue(String value) {
        if (value == null) return null;
        for (TransferStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown transfer status: " + value);
    }
}
