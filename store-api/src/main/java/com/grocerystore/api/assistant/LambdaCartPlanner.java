package com.grocerystore.api.assistant;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grocerystore.api.assistant.AssistantDtos.Turn;
import com.grocerystore.api.common.StoreProperties;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/** Asks the model through a small Lambda function, which is the only thing that holds the model access. */
@Component
@ConditionalOnExpression("!'${store.assistant-function:}'.isEmpty()")
public class LambdaCartPlanner implements CartPlanner {

    private final LambdaClient lambda;
    private final String functionName;
    private final ObjectMapper json;

    public LambdaCartPlanner(StoreProperties properties, ObjectMapper json) {
        this.functionName = properties.assistantFunction();
        this.json = json;
        this.lambda = LambdaClient.builder()
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(40)))
                .build();
    }

    @Override
    public Plan plan(String instructions, List<Turn> history, String message) {
        try {
            String request = json.writeValueAsString(
                    Map.of("instructions", instructions, "history", history, "message", message));
            InvokeResponse response = lambda.invoke(r -> r.functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(request)));
            String body = response.payload().asUtf8String();
            if (response.functionError() != null) {
                throw new IllegalStateException("Assistant function failed: " + body);
            }
            return read(json.readTree(body));
        } catch (IOException e) {
            throw new IllegalStateException("Assistant answer could not be read", e);
        }
    }

    private static Plan read(JsonNode root) {
        List<PlannedItem> items = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            JsonNode productId = item.path("productId");
            JsonNode quantity = item.path("quantity");
            if (productId.canConvertToLong() && quantity.canConvertToInt()) {
                items.add(new PlannedItem(productId.asLong(), quantity.asInt(), item.path("note").asText("")));
            }
        }
        List<String> notStocked = new ArrayList<>();
        for (JsonNode name : root.path("notStocked")) {
            if (name.isTextual() && !name.asText().isBlank()) {
                notStocked.add(name.asText());
            }
        }
        JsonNode budget = root.path("budgetRupees");
        JsonNode goal = root.path("nutritionGoal");
        NutritionGoal nutritionGoal = !goal.isObject() ? null : new NutritionGoal(positive(goal, "people"),
                positive(goal, "days"), positive(goal, "proteinG"), positive(goal, "fatG"),
                positive(goal, "carbsG"), positive(goal, "fiberG"), positive(goal, "kcal"));
        // A goal with no nutrient in it is no goal.
        if (nutritionGoal != null && nutritionGoal.proteinG() == null && nutritionGoal.fatG() == null
                && nutritionGoal.carbsG() == null && nutritionGoal.fiberG() == null && nutritionGoal.kcal() == null) {
            nutritionGoal = null;
        }
        return new Plan(root.path("reply").asText(""), items, notStocked,
                budget.canConvertToInt() && budget.asInt() > 0 ? budget.asInt() : null, nutritionGoal);
    }

    private static Integer positive(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.canConvertToInt() && value.asInt() > 0 ? value.asInt() : null;
    }
}
