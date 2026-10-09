package com.grocerystore.api.common;

public record ApiError(String error, String message, Object details) {
}
