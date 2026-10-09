package com.grocerystore.api.common;

/** Mobile numbers the store accepts: Indian (+91) and US (+1). */
public final class PhoneNumbers {

    public static final String PATTERN = "(\\+91)?[6-9][0-9]{9}|\\+1[2-9][0-9]{9}";
    public static final String MESSAGE = "must be an Indian (+91) or US (+1) mobile number";

    private PhoneNumbers() {
    }

    /** The stored form always carries the country code. A bare 10 digit number is taken as Indian. */
    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        return trimmed.startsWith("+") ? trimmed : "+91" + trimmed;
    }
}
