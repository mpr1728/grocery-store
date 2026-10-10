package com.grocerystore.api.assistant;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AssistantDtos {

    private AssistantDtos() {
    }

    public record AskRequest(
            @NotBlank @Size(max = 500) String message,
            @Size(max = 10) @Valid List<Turn> history) {
    }

    /**
     * One earlier message in the conversation, kept by the browser and sent back for context. An assistant
     * turn carries the list the shopper was shown, so a follow-up can change that list.
     */
    public record Turn(@NotBlank @Pattern(regexp = "user|assistant") String role, @NotBlank @Size(max = 1000) String text,
            @Size(max = 100) @Valid List<TurnItem> items) {
    }

    public record TurnItem(@NotNull Long productId, @NotNull @Min(1) @Max(50) Integer quantity) {
    }

    /**
     * nutrition is null when none of the suggested products has nutrition figures. notes are remarks the
     * store adds itself, such as having trimmed the list to a budget. They are shown apart from the reply
     * and are not sent back as conversation.
     */
    public record AssistantReply(String reply, List<Suggested> items, List<String> notStocked,
            NutritionSummary nutrition, List<String> notes) {
    }

    /** Grams of each nutrient, and kilocalories. A null field means it was not stated or is not known. */
    public record Macros(Integer proteinG, Integer fatG, Integer carbsG, Integer fiberG, Integer kcal) {
    }

    /**
     * What the whole list contains, worked out from pack weights and per 100 g figures. When the shopper gave
     * targets, perPersonPerDay and target allow a comparison. itemsNotCounted have no figures on file.
     */
    public record NutritionSummary(Macros total, Integer people, Integer days, Macros perPersonPerDay,
            Macros target, int itemsNotCounted) {
    }

    public record Suggested(Long productId, String name, String unitLabel, int pricePaise, int quantity, String note) {
    }
}
