package com.grocerystore.api.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.grocerystore.api.catalog.ProductService.ProductDetail;
import com.grocerystore.api.common.DocumentService;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService products;
    private final DocumentService documents;

    public ProductController(ProductService products, DocumentService documents) {
        this.products = products;
        this.documents = documents;
    }

    /** Everything a shopper can see about one product, including the evidence for how it was grown. */
    @GetMapping("/hubs/{hubId}/products/{productId}")
    public ProductDetail detail(@PathVariable Long hubId, @PathVariable Long productId) {
        return products.detail(hubId, productId);
    }

    /** Staff: sets how a product was grown and, optionally, uploads the certificate. */
    @PostMapping(value = "/products/{productId}/growing", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setGrowing(@PathVariable Long productId, @RequestParam(required = false) String growingMethod,
            @RequestParam(required = false) String certifier,
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) MultipartFile certificate) {
        products.setGrowing(productId, growingMethod, certifier, certificateNumber, documents.store(certificate));
    }
}
