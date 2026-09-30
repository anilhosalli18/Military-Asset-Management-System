package com.mams.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EquipmentCategory {
    vehicle("vehicle"),
    weapon("weapon"),
    ammunition("ammunition"),
    other("other");

    private final String value;

    EquipmentCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EquipmentCategory fromValue(String value) {
        if (value == null) return null;
        for (EquipmentCategory category : values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown equipment category: " + value);
    }
}
