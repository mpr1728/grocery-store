package com.grocerystore.api.assistant;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.common.NotFoundException;

/** What the assistant keeps about a shopper between conversations. */
@Service
public class MemoryService {

    private static final int MAX_FACTS = 15;
    private static final int MAX_LENGTH = 160;

    public record Fact(Long id, String fact) {
    }

    private final AssistantMemoryRepository memories;
    private final Clock clock;

    public MemoryService(AssistantMemoryRepository memories, Clock clock) {
        this.memories = memories;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Fact> facts(Long customerId) {
        return memories.findByCustomerIdOrderById(customerId).stream()
                .map(m -> new Fact(m.getId(), m.getFact()))
                .toList();
    }

    /** Stores the facts that are new and returns just those. The oldest are dropped beyond the limit. */
    @Transactional
    public List<String> remember(Long customerId, List<String> facts) {
        List<AssistantMemory> known = new ArrayList<>(memories.findByCustomerIdOrderById(customerId));
        List<String> added = new ArrayList<>();
        Instant now = Instant.now(clock);
        for (String raw : facts) {
            String fact = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
            if (fact.length() > MAX_LENGTH) {
                fact = fact.substring(0, MAX_LENGTH);
            }
            String wanted = fact;
            if (fact.isEmpty() || known.stream().anyMatch(m -> m.getFact().equalsIgnoreCase(wanted))) {
                continue;
            }
            known.add(memories.save(new AssistantMemory(customerId, fact, now)));
            added.add(fact);
        }
        while (known.size() > MAX_FACTS) {
            memories.delete(known.remove(0));
        }
        return added;
    }

    @Transactional
    public void forget(Long customerId, Long factId) {
        AssistantMemory memory = memories.findById(factId)
                .filter(m -> m.getCustomerId().equals(customerId))
                .orElseThrow(() -> new NotFoundException("That note was not found"));
        memories.delete(memory);
    }

    @Transactional
    public void forgetAll(Long customerId) {
        memories.deleteAllFor(customerId);
    }
}
