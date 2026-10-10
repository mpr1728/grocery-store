package com.grocerystore.api.assistant;

import java.util.List;

import com.grocerystore.api.assistant.AssistantDtos.Turn;

/** Turns a shopper's request into a draft cart. The answer is untrusted and is checked by AssistantService. */
public interface CartPlanner {

    Plan plan(String instructions, List<Turn> history, String message);

    /**
     * budgetRupees is the spending limit the shopper stated, or null when they gave none.
     * goal holds nutrition targets the shopper stated, or null.
     */
    record Plan(String reply, List<PlannedItem> items, List<String> notStocked, Integer budgetRupees,
            NutritionGoal goal, List<String> remember) {
    }

    /** Daily targets per person, for a number of people and days. Any field may be null. */
    record NutritionGoal(Integer people, Integer days, Integer proteinG, Integer fatG, Integer carbsG,
            Integer fiberG, Integer kcal) {
    }

    /** gramsUsed is the amount the household is expected to eat over the period, or null when not planned. */
    record PlannedItem(long productId, int quantity, String note, Integer gramsUsed) {
    }
}
