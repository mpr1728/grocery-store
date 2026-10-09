package com.grocerystore.api.common;

public class UnavailableException extends RuntimeException {

    public UnavailableException(String message) {
        super(message);
    }
}
