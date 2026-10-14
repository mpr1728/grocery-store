package com.grocerystore.api.fashion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.fashion.FashionDtos.CatalogView;
import com.grocerystore.api.fashion.FashionDtos.DesignRequest;
import com.grocerystore.api.fashion.FashionDtos.FitView;
import com.grocerystore.api.fashion.FashionDtos.MeasurementsRequest;
import com.grocerystore.api.fashion.FashionDtos.OrderView;
import com.grocerystore.api.fashion.FashionDtos.PlaceOrderRequest;
import com.grocerystore.api.fashion.FashionDtos.QuoteView;

import jakarta.validation.Valid;

/** Panyaloka Fashion. Uses the same accounts as the grocery store. */
@RestController
@RequestMapping("/api/fashion")
public class FashionController {

    private final FashionService fashion;
    private final AccountService accounts;

    public FashionController(FashionService fashion, AccountService accounts) {
        this.fashion = fashion;
        this.accounts = accounts;
    }

    @GetMapping("/catalog")
    public CatalogView catalog() {
        return fashion.catalog();
    }

    @PostMapping("/quote")
    public QuoteView quote(@Valid @RequestBody DesignRequest design) {
        return fashion.quote(design);
    }

    @PostMapping("/fit")
    public FitView fit(@Valid @RequestBody MeasurementsRequest measurements) {
        return fashion.fit(measurements);
    }

    /** 204 when the customer has not saved measurements yet. */
    @GetMapping("/me/measurements")
    public ResponseEntity<FitView> savedFit(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return fashion.savedFit(accounts.require(authorization).getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/me/measurements")
    public FitView saveFit(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody MeasurementsRequest measurements) {
        return fashion.saveFit(accounts.require(authorization).getId(), measurements);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PlaceOrderRequest request) {
        return fashion.place(accounts.require(authorization), request);
    }

    @GetMapping("/me/orders")
    public List<OrderView> myOrders(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return fashion.ordersOf(accounts.require(authorization));
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderView get(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNumber) {
        return fashion.get(accounts.require(authorization), orderNumber);
    }

    @PostMapping("/orders/{orderNumber}/cancel")
    public OrderView cancel(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String orderNumber) {
        return fashion.cancel(accounts.require(authorization), orderNumber);
    }

    // Studio console. No sign-in yet, the same open item as the grocery hub console.

    @GetMapping("/studio/orders")
    public List<OrderView> studioQueue() {
        return fashion.studioQueue();
    }

    @PostMapping("/studio/orders/{orderNumber}/advance")
    public OrderView advance(@PathVariable String orderNumber) {
        return fashion.advance(orderNumber);
    }
}
