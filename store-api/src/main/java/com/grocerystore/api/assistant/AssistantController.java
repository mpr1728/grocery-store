package com.grocerystore.api.assistant;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.assistant.AssistantDtos.AskRequest;
import com.grocerystore.api.assistant.AssistantDtos.AssistantReply;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hubs/{hubId}/assistant")
public class AssistantController {

    private final AssistantService assistant;
    private final AccountService accounts;

    public AssistantController(AssistantService assistant, AccountService accounts) {
        this.assistant = assistant;
        this.accounts = accounts;
    }

    // Signed-in customers only, so the cost of each message is tied to an account with a daily limit.
    @PostMapping
    public AssistantReply ask(@PathVariable Long hubId, @Valid @RequestBody AskRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return assistant.ask(hubId, accounts.require(authorization), request);
    }
}
