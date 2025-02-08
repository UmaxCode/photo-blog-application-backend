package org.umaxcode.domain.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OwnershipType {

    OWN_PHOTO("own-photo"),
    OTHERS_PHOTO("others-photo");

    private final String name;

    // Method to get PhotoType from string value
    public static OwnershipType fromString(String value) {
        for (OwnershipType ownershipType : OwnershipType.values()) {
            if (ownershipType.name.equalsIgnoreCase(value)) {
                return ownershipType;
            }
        }
        throw new IllegalStateException("Invalid ownership type: " + value);
    }

}
