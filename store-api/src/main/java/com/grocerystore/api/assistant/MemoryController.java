package com.grocerystore.api.assistant;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.assistant.MemoryService.Fact;

/** Lets a shopper see and delete what the assistant remembers about them. */
@RestController
@RequestMapping("/api/me/assistant-memory")
public class MemoryController {

    private final MemoryService memory;
    private final AccountService accounts;

    public MemoryController(MemoryService memory, AccountService accounts) {
        this.memory = memory;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Fact> facts(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return memory.facts(accounts.require(authorization).getId());
    }

    @DeleteMapping("/{factId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forget(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long factId) {
        memory.forget(accounts.require(authorization).getId(), factId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgetAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        memory.forgetAll(accounts.require(authorization).getId());
    }
}
