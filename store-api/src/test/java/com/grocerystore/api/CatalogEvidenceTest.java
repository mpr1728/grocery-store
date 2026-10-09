package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import com.grocerystore.api.catalog.CandidateService;
import com.grocerystore.api.catalog.CatalogCandidate;
import com.grocerystore.api.catalog.CatalogCandidateRepository;
import com.grocerystore.api.catalog.CatalogItem;
import com.grocerystore.api.catalog.HubProductRepository;
import com.grocerystore.api.catalog.Product;
import com.grocerystore.api.catalog.ProductService;
import com.grocerystore.api.catalog.ProductService.ProductDetail;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.Document;
import com.grocerystore.api.common.DocumentService;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.hub.Hub;
import com.grocerystore.api.hub.HubRepository;
import com.grocerystore.api.inventory.InventoryService;
import com.grocerystore.api.inventory.StockBatch;

@SpringBootTest
@ActiveProfiles("test")
class CatalogEvidenceTest {

    @Autowired
    CandidateService candidateService;
    @Autowired
    CatalogCandidateRepository candidates;
    @Autowired
    ProductService productService;
    @Autowired
    InventoryService inventory;
    @Autowired
    DocumentService documents;
    @Autowired
    HubRepository hubs;
    @Autowired
    HubProductRepository hubProducts;
    @Autowired
    Clock clock;

    Hub hub;
    LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(clock);
        hub = hubs.save(new Hub("C-" + System.nanoTime(), "Test Hub", "Hyderabad", 3000));
    }

    @Test
    void anApprovedCandidateGoesOnSaleWithItsPackDetails() {
        CatalogCandidate candidate = candidate();

        Product product = candidateService.approve(hub.getId(), candidate.getId(), 24900, "VEG", "Staples", "1 kg");

        CatalogItem item = catalogItem(product);
        assertThat(item.name()).isEqualTo("Organic Tur Dal");
        assertThat(item.brand()).isEqualTo("Test Farms");
        assertThat(item.imageUrl()).isEqualTo("media/off/" + candidate.getBarcode() + ".jpg");
        assertThat(item.pricePaise()).isEqualTo(24900);
        assertThat(item.available()).isZero();
        assertThat(productService.detail(hub.getId(), product.getId()).ingredients()).isEqualTo("Pigeon peas");
        // A candidate is decided once.
        assertThatThrownBy(() -> candidateService.approve(hub.getId(), candidate.getId(), 24900, "VEG", "Staples", "1 kg"))
                .isInstanceOf(NotFoundException.class);
        assertThat(candidateService.waiting().candidates()).noneMatch(c -> c.getId().equals(candidate.getId()));
    }

    @Test
    void aRejectedCandidateLeavesTheReviewList() {
        CatalogCandidate candidate = candidate();
        candidateService.reject(candidate.getId());
        assertThat(candidateService.waiting().candidates()).noneMatch(c -> c.getId().equals(candidate.getId()));
    }

    @Test
    void growingMethodAndBatchTestShowAsEvidenceWithTheirDocuments() {
        Product product = candidateService.approve(hub.getId(), candidate().getId(), 24900, "VEG", "Staples", "1 kg");
        StockBatch tested = inventory.receive(hub.getId(), product.getId(), 10, today.plusDays(30), "A-01", null);
        inventory.receive(hub.getId(), product.getId(), 5, today.plusDays(60), "A-02", null);
        assertThat(catalogItem(product).testedBatches()).isZero();

        Long certificate = documents.store(pdf("certificate.pdf"));
        productService.setGrowing(product.getId(), "CERTIFIED_ORGANIC", "Test Certifier", "ORG-123", certificate);
        Long report = documents.store(pdf("../../lab report (1).pdf"));
        inventory.recordTest(hub.getId(), tested.getId(), "Test Lab", today, "NONE_DETECTED", report);

        CatalogItem item = catalogItem(product);
        assertThat(item.growingMethod()).isEqualTo("CERTIFIED_ORGANIC");
        assertThat(item.testedBatches()).isEqualTo(1);

        ProductDetail detail = productService.detail(hub.getId(), product.getId());
        assertThat(detail.certifier()).isEqualTo("Test Certifier");
        assertThat(detail.certificateDocumentId()).isEqualTo(certificate);
        assertThat(detail.available()).isEqualTo(15);
        assertThat(detail.batches()).hasSize(2);
        assertThat(detail.batches().get(0).testResult()).isEqualTo("NONE_DETECTED");
        assertThat(detail.batches().get(0).testDocumentId()).isEqualTo(report);
        assertThat(detail.batches().get(1).testResult()).isNull();

        Document stored = documents.require(report);
        assertThat(stored.getFileName()).isEqualTo("lab report _1_.pdf");
        assertThat(stored.getContent()).hasSize(4);
    }

    @Test
    void evidenceInputIsChecked() {
        Product product = candidateService.approve(hub.getId(), candidate().getId(), 24900, "VEG", "Staples", "1 kg");
        StockBatch batch = inventory.receive(hub.getId(), product.getId(), 10, null, "A-01", null);

        assertThatThrownBy(() -> productService.setGrowing(product.getId(), "PESTICIDE_FREE", null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> productService.setGrowing(product.getId(), "CERTIFIED_ORGANIC", null, null, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> inventory.recordTest(hub.getId(), batch.getId(), "Lab", today.plusDays(1), "NONE_DETECTED", null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> documents.store(new MockMultipartFile("file", "run.exe", "application/x-msdownload", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);

        productService.setGrowing(product.getId(), "GROWER_DECLARED", null, null, null);
        productService.setGrowing(product.getId(), "", null, null, null);
        assertThat(catalogItem(product).growingMethod()).isNull();
    }

    private CatalogCandidate candidate() {
        String barcode = String.valueOf(8900000000000L + (System.nanoTime() % 1_000_000_000L));
        return candidates.save(new CatalogCandidate(barcode, "Organic Tur Dal", "Test Farms", "1 kg", "Pulses", "VEG",
                "Pigeon peas", "Organic", "media/off/" + barcode + ".jpg", "OFF",
                "https://world.openfoodfacts.org/product/" + barcode, Instant.now(clock)));
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[] {37, 80, 68, 70});
    }

    private CatalogItem catalogItem(Product product) {
        return hubProducts.findCatalog(hub.getId(), today, null, null).stream()
                .filter(i -> i.productId().equals(product.getId()))
                .findFirst().orElseThrow();
    }
}
