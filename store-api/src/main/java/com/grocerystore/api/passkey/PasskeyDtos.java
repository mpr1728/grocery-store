package com.grocerystore.api.passkey;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasskeyDtos {

    private PasskeyDtos() {
    }

    /** optionsJson is passed to the browser's navigator.credentials call, after its binary fields are decoded. */
    public record CeremonyOptions(String ceremonyId, String optionsJson) {
    }

    /** credentialJson is the browser's answer, with its binary fields as base64url. */
    public record CeremonyResponse(@NotBlank @Size(max = 100) String ceremonyId,
            @NotBlank @Size(max = 20000) String credentialJson,
            @Size(max = 60) String label) {
    }

    public record PasskeyView(Long id, String label, Instant createdAt, Instant lastUsedAt) {
    }
}
