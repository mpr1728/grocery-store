package com.grocerystore.api.assistant;

import java.util.List;

import com.grocerystore.api.assistant.AssistantDtos.Turn;

/** Turns a shopper's request into a draft cart. The answer is untrusted and is checked by AssistantService. */
public interface CartPlanner {

    Plan plan(String instructions, List<Turn> history, String message);

    record Plan(String reply, List<PlannedItem> items, List<String> notStocked) {
    }

    record PlannedItem(long productId, int quantity, String note) {
    }
}
