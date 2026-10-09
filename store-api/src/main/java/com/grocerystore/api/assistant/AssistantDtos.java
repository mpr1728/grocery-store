package com.grocerystore.api.assistant;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AssistantDtos {

    private AssistantDtos() {
    }

    public record AskRequest(
            @NotBlank @Size(max = 500) String message,
            @Size(max = 10) @Valid List<Turn> history) {
    }

    /** One earlier message in the conversation, kept by the browser and sent back for context. */
    public record Turn(@NotBlank @Pattern(regexp = "user|assistant") String role, @NotBlank @Size(max = 1000) String text) {
    }

    public record AssistantReply(String reply, List<Suggested> items, List<String> notStocked) {
    }

    public record Suggested(Long productId, String name, String unitLabel, int pricePaise, int quantity, String note) {
    }
}
