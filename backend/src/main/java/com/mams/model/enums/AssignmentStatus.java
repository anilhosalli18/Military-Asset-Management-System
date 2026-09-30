package com.mams.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssignmentStatus {
    assigned("assigned"),
    expended("expended"),
    returned("returned");

    private final String value;

    AssignmentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AssignmentStatus fromValue(String value) {
        if (value == null) return null;
        for (AssignmentStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown assignment status: " + value);
    }
}
