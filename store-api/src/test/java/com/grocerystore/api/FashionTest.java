package com.grocerystore.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.grocerystore.api.account.AccountDtos.SignUpRequest;
import com.grocerystore.api.account.AccountService;
import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.fashion.BodyShape;
import com.grocerystore.api.fashion.FashionDtos.CatalogView;
import com.grocerystore.api.fashion.FashionDtos.DesignRequest;
import com.grocerystore.api.fashion.FashionDtos.MeasurementsRequest;
import com.grocerystore.api.fashion.FashionDtos.OrderView;
import com.grocerystore.api.fashion.FashionDtos.PlaceOrderRequest;
import com.grocerystore.api.fashion.FashionDtos.QuoteView;
import com.grocerystore.api.fashion.FashionOrderStatus;
import com.grocerystore.api.fashion.FashionService;

@SpringBootTest
@ActiveProfiles("test")
class FashionTest {

    @Autowired
    FashionService fashion;
    @Autowired
    AccountService accounts;

    private static final MeasurementsRequest PEAR = new MeasurementsRequest(86, 70, 98, 37, 160);

    private Customer member() {
        String phone = "9" + (100000000 + ThreadLocalRandom.current().nextInt(899999999));
        String token = accounts.signUp(new SignUpRequest("Fashion Tester", phone, "f" + phone + "@example.com", "password1")).token();
        return accounts.require("Bearer " + token);
    }

    private static DesignRequest kurti(String fit, String fabric, String colour) {
        return new DesignRequest("KURTI", "ROUND", "ELBOW", "KNEE", fit, fabric, colour);
    }

    private static PlaceOrderRequest order(DesignRequest design) {
        return new PlaceOrderRequest(design, PEAR, "9876543210", "Flat 4B, Gachibowli", null, true);
    }

    @Test
    void catalogListsEveryPartForEveryGarment() {
        CatalogView catalog = fashion.catalog();
        assertThat(catalog.garments()).extracting(g -> g.code()).containsExactly("KURTI", "TOP", "FROCK");
        catalog.garments().forEach(g -> {
            assertThat(g.parts()).extracting(p -> p.part()).containsExactly("NECKLINE", "SLEEVE", "LENGTH", "FIT");
            // Exactly one preselected choice per part, so the studio always opens on a complete design.
            g.parts().forEach(p -> assertThat(p.options()).filteredOn(o -> o.preselected()).hasSize(1));
        });
        assertThat(catalog.colours()).hasSize(10);
    }

    @Test
    void quoteAddsSurchargesAndShipsFreeAboveTheThreshold() {
        QuoteView plain = fashion.quote(kurti("STRAIGHT", "MULMUL", "INDIGO_DEEP"));
        assertThat(plain.basePricePaise()).isEqualTo(149000);
        assertThat(plain.extras()).isEmpty();
        assertThat(plain.shippingPaise()).isEqualTo(9900);
        assertThat(plain.totalPaise()).isEqualTo(158900);

        // Flared 300 plus khadi 350 takes the kurti to 2,140, over the free shipping line of 1,999.
        QuoteView dressier = fashion.quote(kurti("FLARED", "KHADI", "MADDER"));
        assertThat(dressier.extras()).extracting(l -> l.paise()).containsExactly(30000, 35000);
        assertThat(dressier.shippingPaise()).isZero();
        assertThat(dressier.totalPaise()).isEqualTo(149000 + 30000 + 35000);
    }

    @Test
    void optionFromAnotherGarmentIsRefused() {
        // Peplum is a top fit, not a kurti one.
        assertThatThrownBy(() -> fashion.quote(kurti("PEPLUM", "MULMUL", "INDIGO_DEEP")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Fit PEPLUM");
        assertThatThrownBy(() -> fashion.quote(kurti("STRAIGHT", "SILK", "INDIGO_DEEP")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void bodyShapesFollowTheMeasurements() {
        assertThat(BodyShape.of(86, 70, 98)).isEqualTo(BodyShape.PEAR);
        assertThat(BodyShape.of(100, 80, 88)).isEqualTo(BodyShape.INVERTED_TRIANGLE);
        assertThat(BodyShape.of(92, 66, 96)).isEqualTo(BodyShape.HOURGLASS);
        assertThat(BodyShape.of(88, 76, 92)).isEqualTo(BodyShape.RECTANGLE);
        assertThat(BodyShape.of(96, 98, 100)).isEqualTo(BodyShape.ROUND);
    }

    @Test
    void placingAnOrderSavesMeasurementsAndOnlyTheOwnerSeesIt() {
        Customer owner = member();
        OrderView placed = fashion.place(owner, order(kurti("A_LINE", "POPLIN", "TURMERIC")));

        assertThat(placed.orderNumber()).startsWith("F");
        assertThat(placed.status()).isEqualTo(FashionOrderStatus.PLACED);
        assertThat(placed.shapeLabel()).isEqualTo("Pear");
        assertThat(placed.extrasPaise()).isEqualTo(15000 + 10000);
        assertThat(placed.phone()).isEqualTo("+919876543210");
        assertThat(fashion.savedFit(owner.getId())).get().extracting(f -> f.bodyShape()).isEqualTo(BodyShape.PEAR);
        assertThat(fashion.ordersOf(owner)).extracting(OrderView::orderNumber).containsExactly(placed.orderNumber());

        Customer stranger = member();
        assertThatThrownBy(() -> fashion.get(stranger, placed.orderNumber())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> fashion.cancel(stranger, placed.orderNumber())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancelWorksOnlyBeforeCutting() {
        Customer owner = member();
        String early = fashion.place(owner, order(kurti("STRAIGHT", "MULMUL", "LAC"))).orderNumber();
        assertThat(fashion.cancel(owner, early).status()).isEqualTo(FashionOrderStatus.CANCELLED);
        assertThat(fashion.studioQueue()).extracting(OrderView::orderNumber).doesNotContain(early);

        String late = fashion.place(owner, order(kurti("STRAIGHT", "MULMUL", "LAC"))).orderNumber();
        assertThat(fashion.advance(late).status()).isEqualTo(FashionOrderStatus.CUTTING);
        assertThatThrownBy(() -> fashion.cancel(owner, late)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void studioAdvancesThroughEveryStageAndStopsAtDelivered() {
        String number = fashion.place(member(), order(kurti("STRAIGHT", "MULMUL", "CUTCH"))).orderNumber();
        FashionOrderStatus status = FashionOrderStatus.PLACED;
        while (status != FashionOrderStatus.DELIVERED) {
            status = fashion.advance(number).status();
        }
        assertThatThrownBy(() -> fashion.advance(number)).isInstanceOf(BadRequestException.class);
    }
}
