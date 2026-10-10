package com.grocerystore.api.cart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.catalog.ProductRepository;

/** Keeps a signed-in customer's cart between visits. Prices and stock are checked again at checkout. */
@Service
public class SavedCartService {

    public record Line(Long productId, int quantity) {
    }

    private final CartLineRepository lines;
    private final ProductRepository products;

    public SavedCartService(CartLineRepository lines, ProductRepository products) {
        this.lines = lines;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<Line> get(Long customerId) {
        return lines.findByCustomerIdOrderById(customerId).stream()
                .map(l -> new Line(l.getProductId(), l.getQuantity()))
                .toList();
    }

    /** Replaces the saved cart. Products that do not exist are dropped and a repeated product keeps its last quantity. */
    @Transactional
    public void replace(Long customerId, List<Line> cart) {
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (Line line : cart) {
            wanted.put(line.productId(), line.quantity());
        }
        lines.deleteAllFor(customerId);
        wanted.forEach((productId, quantity) -> {
            if (products.existsById(productId)) {
                lines.save(new CartLine(customerId, productId, quantity));
            }
        });
    }

    @Transactional
    public void clear(Long customerId) {
        lines.deleteAllFor(customerId);
    }
}
