package com.grocerystore.api.order;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.cart.SavedCartService;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.order.OrderDtos.OrderView;
import com.grocerystore.api.order.OrderDtos.PlaceOrderRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orders;
    private final AccountService accounts;
    private final SavedCartService carts;

    public OrderController(OrderService orders, AccountService accounts, SavedCartService carts) {
        this.orders = orders;
        this.accounts = accounts;
        this.carts = carts;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@Valid @RequestBody PlaceOrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long memberId = accounts.signedIn(authorization).map(Customer::getId).orElse(null);
        OrderView order = orders.place(request, memberId);
        if (memberId != null) {
            // The order has what was in the cart, so the saved copy starts empty again.
            carts.clear(memberId);
        }
        return order;
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderView get(@PathVariable String orderNumber) {
        return orders.get(orderNumber);
    }

    @PostMapping("/orders/{orderNumber}/advance")
    public OrderView advance(@PathVariable String orderNumber) {
        return orders.advance(orderNumber);
    }

    @GetMapping("/hubs/{hubId}/orders")
    public List<OrderView> recentForHub(@PathVariable Long hubId) {
        return orders.recentForHub(hubId);
    }
}
