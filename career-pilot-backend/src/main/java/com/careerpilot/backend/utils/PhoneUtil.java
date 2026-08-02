package com.careerpilot.backend.utils;

/**
 * Utility methods for phone number handling.
 */
public final class PhoneUtil {

    private PhoneUtil() {}

    /**
     * Normalises a phone number to E.164 format (always with a leading '+').
     * e.g. "201063659918" → "+201063659918", "+201063659918" → "+201063659918"
     */
    public static String normalise(String phoneNumber) {
        if (phoneNumber == null) return null;
        phoneNumber = phoneNumber.trim();
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }
        return phoneNumber;
    }
}
