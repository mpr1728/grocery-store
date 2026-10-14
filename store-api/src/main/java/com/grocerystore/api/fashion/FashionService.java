package com.grocerystore.api.fashion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grocerystore.api.common.BadRequestException;
import com.grocerystore.api.common.NotFoundException;
import com.grocerystore.api.common.PhoneNumbers;
import com.grocerystore.api.customer.Customer;
import com.grocerystore.api.customer.CustomerRepository;
import com.grocerystore.api.fashion.FashionDtos.CatalogView;
import com.grocerystore.api.fashion.FashionDtos.Choice;
import com.grocerystore.api.fashion.FashionDtos.ColourView;
import com.grocerystore.api.fashion.FashionDtos.DesignRequest;
import com.grocerystore.api.fashion.FashionDtos.FabricView;
import com.grocerystore.api.fashion.FashionDtos.FitView;
import com.grocerystore.api.fashion.FashionDtos.GarmentView;
import com.grocerystore.api.fashion.FashionDtos.MeasurementsRequest;
import com.grocerystore.api.fashion.FashionDtos.OptionView;
import com.grocerystore.api.fashion.FashionDtos.OrderView;
import com.grocerystore.api.fashion.FashionDtos.PartView;
import com.grocerystore.api.fashion.FashionDtos.PlaceOrderRequest;
import com.grocerystore.api.fashion.FashionDtos.PriceLine;
import com.grocerystore.api.fashion.FashionDtos.QuoteView;

@Service
public class FashionService {

    /** The parts every design chooses, in the order the studio shows them. */
    public enum Part {
        NECKLINE("Neckline"), SLEEVE("Sleeve"), LENGTH("Length"), FIT("Fit");

        public final String label;

        Part(String label) {
            this.label = label;
        }
    }

    private static final String ORDER_NUMBER_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final List<FashionOrderStatus> OPEN = List.of(FashionOrderStatus.PLACED, FashionOrderStatus.CUTTING,
            FashionOrderStatus.STITCHING, FashionOrderStatus.CHECKED, FashionOrderStatus.DISPATCHED);

    private final GarmentRepository garments;
    private final GarmentOptionRepository options;
    private final FabricRepository fabrics;
    private final ColourRepository colours;
    private final MeasurementsRepository measurements;
    private final FashionOrderRepository orders;
    private final CustomerRepository customers;
    private final Clock clock;
    private final int shippingPaise;
    private final int freeShippingAbovePaise;

    public FashionService(GarmentRepository garments, GarmentOptionRepository options, FabricRepository fabrics,
            ColourRepository colours, MeasurementsRepository measurements, FashionOrderRepository orders,
            CustomerRepository customers, Clock clock,
            @Value("${fashion.shipping-paise}") int shippingPaise,
            @Value("${fashion.free-shipping-above-paise}") int freeShippingAbovePaise) {
        this.garments = garments;
        this.options = options;
        this.fabrics = fabrics;
        this.colours = colours;
        this.measurements = measurements;
        this.orders = orders;
        this.customers = customers;
        this.clock = clock;
        this.shippingPaise = shippingPaise;
        this.freeShippingAbovePaise = freeShippingAbovePaise;
    }

    @Transactional(readOnly = true)
    public CatalogView catalog() {
        Map<String, Map<Part, List<OptionView>>> byGarment = new LinkedHashMap<>();
        for (GarmentOption option : options.findAllByOrderByGarmentCodeAscPartAscSortOrderAsc()) {
            byGarment.computeIfAbsent(option.getGarmentCode(), code -> new EnumMap<>(Part.class))
                    .computeIfAbsent(Part.valueOf(option.getPart()), part -> new ArrayList<>())
                    .add(new OptionView(option.getCode(), option.getLabel(), option.getSurchargePaise(), option.isPreselected()));
        }
        List<GarmentView> garmentViews = garments.findByActiveTrueOrderBySortOrder().stream()
                .map(g -> new GarmentView(g.getCode(), g.getName(), g.getDescription(), g.getBasePricePaise(),
                        g.getMakingDays(), byGarment.getOrDefault(g.getCode(), Map.of()).entrySet().stream()
                                .map(e -> new PartView(e.getKey().name(), e.getKey().label, e.getValue()))
                                .toList()))
                .toList();
        List<FabricView> fabricViews = fabrics.findByActiveTrueOrderBySortOrder().stream()
                .map(f -> new FabricView(f.getCode(), f.getName(), f.getDescription(), f.getSurchargePaise()))
                .toList();
        List<ColourView> colourViews = colours.findAllByOrderBySortOrder().stream()
                .map(c -> new ColourView(c.getCode(), c.getName(), c.getDyeSource(), c.getHex(), c.isAvailable()))
                .toList();
        return new CatalogView(garmentViews, fabricViews, colourViews, shippingPaise, freeShippingAbovePaise);
    }

    @Transactional(readOnly = true)
    public QuoteView quote(DesignRequest design) {
        return quoteView(price(design));
    }

    /** Reads the shape from measurements without saving them, so a guest can try the fit guide. */
    public FitView fit(MeasurementsRequest m) {
        return fitView(m, null);
    }

    @Transactional(readOnly = true)
    public Optional<FitView> savedFit(Long customerId) {
        return measurements.findById(customerId).map(FashionService::fitView);
    }

    @Transactional
    public FitView saveFit(Long customerId, MeasurementsRequest m) {
        Measurements saved = measurements.findById(customerId).orElseGet(() -> new Measurements(customerId));
        saved.update(m.bustCm(), m.waistCm(), m.hipCm(), m.shoulderCm(), m.heightCm(), Instant.now(clock));
        return fitView(measurements.save(saved));
    }

    @Transactional
    public OrderView place(Customer customer, PlaceOrderRequest request) {
        FashionQuote quote = price(request.design());
        MeasurementsRequest m = request.measurements();
        saveFit(customer.getId(), m);
        String notes = request.notes() == null || request.notes().isBlank() ? null : request.notes().trim();
        FashionOrder order = orders.save(new FashionOrder(newOrderNumber(), customer.getId(), quote, m.bustCm(),
                m.waistCm(), m.hipCm(), m.shoulderCm(), m.heightCm(), PhoneNumbers.normalize(request.phone()),
                request.deliveryAddress().trim(), notes, Instant.now(clock)));
        return view(order, customer);
    }

    @Transactional(readOnly = true)
    public List<OrderView> ordersOf(Customer customer) {
        return orders.findTop50ByCustomerIdOrderByIdDesc(customer.getId()).stream().map(o -> view(o, customer)).toList();
    }

    /** One order, shown only to the customer who placed it. */
    @Transactional(readOnly = true)
    public OrderView get(Customer customer, String orderNumber) {
        FashionOrder order = require(orderNumber);
        if (!order.getCustomerId().equals(customer.getId())) {
            // Same answer as a missing order, so order numbers cannot be probed.
            throw notFound(orderNumber);
        }
        return view(order, customer);
    }

    @Transactional
    public OrderView cancel(Customer customer, String orderNumber) {
        FashionOrder order = require(orderNumber);
        if (!order.getCustomerId().equals(customer.getId())) {
            throw notFound(orderNumber);
        }
        if (!order.getStatus().canCancel()) {
            throw new BadRequestException("Order " + orderNumber + " can no longer be cancelled: the fabric has been cut");
        }
        order.moveTo(FashionOrderStatus.CANCELLED, Instant.now(clock));
        return view(order, customer);
    }

    /** Orders the studio still has to make or deliver, newest first. */
    @Transactional(readOnly = true)
    public List<OrderView> studioQueue() {
        return orders.findTop100ByStatusInOrderByIdDesc(OPEN).stream()
                .map(o -> view(o, customers.findById(o.getCustomerId()).orElseThrow()))
                .toList();
    }

    @Transactional
    public OrderView advance(String orderNumber) {
        FashionOrder order = require(orderNumber);
        if (order.getStatus().isFinal()) {
            throw new BadRequestException("Order " + orderNumber + " is already "
                    + order.getStatus().name().toLowerCase());
        }
        order.moveTo(order.getStatus().next(), Instant.now(clock));
        return view(order, customers.findById(order.getCustomerId()).orElseThrow());
    }

    /** Checks every code in the design against the catalog and works out the price. */
    FashionQuote price(DesignRequest design) {
        Garment garment = garments.findById(design.garment()).filter(Garment::isActive)
                .orElseThrow(() -> new BadRequestException("We do not make garment " + design.garment()));
        Map<Part, Map<String, GarmentOption>> choices = new EnumMap<>(Part.class);
        for (GarmentOption option : options.findByGarmentCode(garment.getCode())) {
            choices.computeIfAbsent(Part.valueOf(option.getPart()), part -> new LinkedHashMap<>())
                    .put(option.getCode(), option);
        }
        GarmentOption neckline = choose(garment, choices, Part.NECKLINE, design.neckline());
        GarmentOption sleeve = choose(garment, choices, Part.SLEEVE, design.sleeve());
        GarmentOption length = choose(garment, choices, Part.LENGTH, design.length());
        GarmentOption fit = choose(garment, choices, Part.FIT, design.fit());
        Fabric fabric = fabrics.findById(design.fabric()).filter(Fabric::isActive)
                .orElseThrow(() -> new BadRequestException("Fabric " + design.fabric() + " is not offered"));
        Colour colour = colours.findById(design.colour())
                .orElseThrow(() -> new BadRequestException("Colour " + design.colour() + " is not in the palette"));
        if (!colour.isAvailable()) {
            throw new BadRequestException(colour.getName() + " is out of stock at the dye house. Please pick another shade");
        }
        int extras = neckline.getSurchargePaise() + sleeve.getSurchargePaise() + length.getSurchargePaise()
                + fit.getSurchargePaise() + fabric.getSurchargePaise();
        int itemPrice = garment.getBasePricePaise() + extras;
        int shipping = itemPrice >= freeShippingAbovePaise ? 0 : shippingPaise;
        LocalDate readyBy = LocalDate.now(clock).plusDays(garment.getMakingDays());
        return new FashionQuote(garment, neckline, sleeve, length, fit, fabric, colour, extras, shipping,
                itemPrice + shipping, readyBy);
    }

    private static GarmentOption choose(Garment garment, Map<Part, Map<String, GarmentOption>> choices, Part part,
            String code) {
        GarmentOption option = choices.getOrDefault(part, Map.of()).get(code);
        if (option == null) {
            throw new BadRequestException(part.label + " " + code + " is not offered on a " + garment.getName().toLowerCase());
        }
        return option;
    }

    private static QuoteView quoteView(FashionQuote q) {
        List<PriceLine> extras = new ArrayList<>();
        for (GarmentOption option : List.of(q.neckline(), q.sleeve(), q.length(), q.fit())) {
            if (option.getSurchargePaise() > 0) {
                extras.add(new PriceLine(Part.valueOf(option.getPart()).label + ": " + option.getLabel(),
                        option.getSurchargePaise()));
            }
        }
        if (q.fabric().getSurchargePaise() > 0) {
            extras.add(new PriceLine("Fabric: " + q.fabric().getName(), q.fabric().getSurchargePaise()));
        }
        return new QuoteView(q.garment().getName(), q.garment().getBasePricePaise(), extras, q.shippingPaise(),
                q.totalPaise(), q.readyBy());
    }

    private static FitView fitView(Measurements m) {
        return fitView(new MeasurementsRequest(m.getBustCm(), m.getWaistCm(), m.getHipCm(), m.getShoulderCm(),
                m.getHeightCm()), m.getUpdatedAt());
    }

    private static FitView fitView(MeasurementsRequest m, Instant updatedAt) {
        BodyShape shape = BodyShape.of(m.bustCm(), m.waistCm(), m.hipCm());
        return new FitView(m, shape, shape.label, shape.advice, shape.suitedFits, updatedAt);
    }

    private static OrderView view(FashionOrder o, Customer customer) {
        List<Choice> choices = List.of(new Choice(Part.NECKLINE.label, o.getNeckline()),
                new Choice(Part.SLEEVE.label, o.getSleeve()), new Choice(Part.LENGTH.label, o.getLength()),
                new Choice(Part.FIT.label, o.getFit()), new Choice("Fabric", o.getFabric()),
                new Choice("Colour", o.getColour()));
        MeasurementsRequest m = new MeasurementsRequest(o.getBustCm(), o.getWaistCm(), o.getHipCm(), o.getShoulderCm(),
                o.getHeightCm());
        return new OrderView(o.getOrderNumber(), o.getStatus(), o.getStatus().canCancel(), customer.getName(),
                o.getGarmentName(), choices, o.getColourHex(), o.getDesignCodes(), m, o.getBodyShape().label,
                o.getBasePricePaise(), o.getExtrasPaise(), o.getShippingPaise(), o.getTotalPaise(),
                o.getContactPhone(), o.getDeliveryAddress(), o.getNotes(), o.getPlacedAt(), o.getUpdatedAt(),
                o.getReadyBy());
    }

    private FashionOrder require(String orderNumber) {
        return orders.findByOrderNumber(orderNumber).orElseThrow(() -> notFound(orderNumber));
    }

    private static NotFoundException notFound(String orderNumber) {
        return new NotFoundException("Order " + orderNumber + " was not found");
    }

    private String newOrderNumber() {
        String day = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyMMdd"));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (true) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                suffix.append(ORDER_NUMBER_ALPHABET.charAt(random.nextInt(ORDER_NUMBER_ALPHABET.length())));
            }
            // F marks a fashion order, so it is never confused with a grocery one.
            String candidate = "F" + day + "-" + suffix;
            if (!orders.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
    }
}
