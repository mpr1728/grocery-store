package com.grocerystore.api.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.catalog.CandidateService.CandidatePage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Staff: review of products found by a catalog import. */
@RestController
@RequestMapping("/api")
public class CandidateController {

    public record ApproveRequest(
            @NotNull @Min(100) @Max(10_000_000) Integer pricePaise,
            @NotBlank @Pattern(regexp = "VEG|NON_VEG") String diet,
            @NotBlank @Size(max = 50) String category,
            @NotBlank @Size(max = 30) String unitLabel) {
    }

    public record Approved(Long productId) {
    }

    private final CandidateService candidates;

    public CandidateController(CandidateService candidates) {
        this.candidates = candidates;
    }

    @GetMapping("/catalog/candidates")
    public CandidatePage waiting() {
        return candidates.waiting();
    }

    @PostMapping("/hubs/{hubId}/catalog/candidates/{candidateId}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public Approved approve(@PathVariable Long hubId, @PathVariable Long candidateId,
            @Valid @RequestBody ApproveRequest request) {
        return new Approved(candidates.approve(hubId, candidateId, request.pricePaise(), request.diet(),
                request.category(), request.unitLabel()).getId());
    }

    @PostMapping("/catalog/candidates/{candidateId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long candidateId) {
        candidates.reject(candidateId);
    }
}
