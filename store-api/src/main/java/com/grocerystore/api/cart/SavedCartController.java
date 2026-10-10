package com.grocerystore.api.cart;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.cart.SavedCartService.Line;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/me/cart")
public class SavedCartController {

    public record CartView(List<Line> lines) {
    }

    public record CartRequest(@NotNull @Size(max = 100) @Valid List<LineRequest> lines) {
    }

    public record LineRequest(@NotNull Long productId, @NotNull @Min(1) @Max(50) Integer quantity) {
    }

    private final SavedCartService carts;
    private final AccountService accounts;

    public SavedCartController(SavedCartService carts, AccountService accounts) {
        this.carts = carts;
        this.accounts = accounts;
    }

    @GetMapping
    public CartView get(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return new CartView(carts.get(accounts.require(authorization).getId()));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replace(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CartRequest request) {
        carts.replace(accounts.require(authorization).getId(),
                request.lines().stream().map(l -> new Line(l.productId(), l.quantity())).toList());
    }
}
