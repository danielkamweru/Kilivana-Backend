package com.kilivana.backend.config;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.entity.BuyerProfile;
import com.kilivana.backend.admin.entity.DriverProfile;
import com.kilivana.backend.admin.entity.FarmerProfile;
import com.kilivana.backend.admin.entity.InspectorProfile;
import com.kilivana.backend.admin.entity.SupplierProfile;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.repository.BuyerProfileRepository;
import com.kilivana.backend.admin.repository.DriverProfileRepository;
import com.kilivana.backend.admin.repository.FarmerProfileRepository;
import com.kilivana.backend.admin.repository.InspectorProfileRepository;
import com.kilivana.backend.admin.repository.SupplierProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.DeliveryStatus;
import com.kilivana.backend.common.enums.DisputeStatus;
import com.kilivana.backend.common.enums.DriverStatus;
import com.kilivana.backend.common.enums.KycStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentMethod;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.VehicleType;
import com.kilivana.backend.common.service.UserReferenceCodeGenerator;
import com.kilivana.backend.ecommerce.dto.DisputeRequest;
import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.ecommerce.entity.Dispute;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderEvent;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Payment;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
import com.kilivana.backend.ecommerce.repository.OrderEventRepository;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.ecommerce.service.DisputeService;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.entity.TrackingEvent;
import com.kilivana.backend.logistics.repository.TrackingEventRepository;
import com.kilivana.backend.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Populates a fresh database with realistic Kenyan demo data so the
 * application does not open empty.
 *
 * <p>The seed is <em>idempotent</em>: every record is keyed on a stable
 * identifier - an email, a category name, a product name within one
 * seller's catalogue, an order reference, a payment reference - and a
 * record is only created when no such key exists. Restarting the backend
 * never duplicates data, and an existing record is never overwritten, so
 * real production data is left untouched.
 *
 * <p>Payments are seeded as <em>initiated</em> M-Pesa and bank payments
 * awaiting provider confirmation, never as successful transactions: the
 * seed must not fabricate money that could be mistaken for a real
 * payment.
 *
 * <p>Enabled with {@code SEED_DATA=true}. The initial administrator uses
 * {@code SEED_ADMIN_EMAIL} and {@code SEED_ADMIN_PASSWORD}; when the
 * password is not supplied the administrator account is not created, and
 * the remaining demo accounts share {@code SEED_DEMO_PASSWORD}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedDataRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReferenceCodeGenerator referenceCodeGenerator;
    private final FarmerProfileRepository farmerProfileRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final SupplierProfileRepository supplierProfileRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final InspectorProfileRepository inspectorProfileRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventRepository orderEventRepository;
    private final PaymentRepository paymentRepository;
    private final DisputeRepository disputeRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final LogisticsService logisticsService;
    private final DisputeService disputeService;

    @Value("${app.seed.admin-email:admin@kilivana.com}")
    private String adminEmail;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.demo-password:Kilivana#2026}")
    private String demoPassword;

    /**
     * When true, a seeded account whose password no longer matches
     * the configured value is reset to it. Off by default so a
     * production deploy never silently changes a password; turn it
     * on for a demo environment where the seeded accounts are the
     * intended logins and must stay reproducible.
     */
    @Value("${app.seed.enforce-passwords:false}")
    private boolean enforcePasswords;

    /** Stable identity of every seeded account. */
    private static final String FARMER_EMAIL = "farmer@kilivana.demo";
    private static final String BUYER_EMAIL = "buyer@kilivana.demo";
    private static final String SUPPLIER_EMAIL = "supplier@kilivana.demo";
    private static final String DRIVER_EMAIL = "driver@kilivana.demo";
    private static final String INSPECTOR_EMAIL = "inspector@kilivana.demo";

    /** Stable identities of the seeded orders and payments. */
    private static final String ORDER_ONE = "ORD-SEED-001";
    private static final String ORDER_TWO = "ORD-SEED-002";
    private static final String ORDER_THREE = "ORD-SEED-003";
    private static final String PAYMENT_ONE = "SEED-MPESA-001";
    private static final String PAYMENT_TWO = "SEED-MPESA-002";
    private static final String PAYMENT_THREE = "SEED-BANK-003";
    private static final String DISPUTE_REASON = "Wrong items";

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        int created = 0;
        created += seedUsersAndProfiles();
        created += seedCategories();
        created += seedProducts();
        created += seedAddresses();
        created += seedOrdersAndPayments();
        created += seedLogistics();
        created += seedDispute();
        log.info("Seed complete: {} record(s) created.", created);
    }

    // ------------------------------------------------------------- users

    /**
     * Creates an account when no account with that email exists. An
     * existing account is left exactly as it is, so a seed run never
     * touches a real user - unless password enforcement is on, in
     * which case a seeded account whose password no longer matches
     * the configured value is reset to it. The result reports whether
     * this run created the account, so the summary counts only real
     * insertions.
     */
    private SeededUser findOrCreateUser(String email, String name, String phone, UserRole role, String password) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (enforcePasswords && !passwordEncoder.matches(password, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(password));
                userRepository.save(user);
            }
            return new SeededUser(user, false);
        }
        User user = User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .status(com.kilivana.backend.common.enums.UserStatus.ACTIVE)
                .verificationStatus(com.kilivana.backend.common.enums.VerificationStatus.NOT_REQUIRED)
                .region("Nairobi")
                .referenceCode(referenceCodeGenerator.nextCode(role))
                .build();
        return new SeededUser(userRepository.save(user), true);
    }

    /** An account the seeder needs, and whether this run created it. */
    private record SeededUser(User user, boolean created) {

        private static int count(SeededUser... seeded) {
            int created = 0;
            for (SeededUser user : seeded) {
                if (user.created()) {
                    created++;
                }
            }
            return created;
        }
    }

    private int seedUsersAndProfiles() {
        int created = 0;

        // The initial administrator. Without SEED_ADMIN_PASSWORD there is
        // nothing safe to put in the account, so it is simply not created.
        if (!adminPassword.isBlank()) {
            SeededUser admin = findOrCreateUser(adminEmail, "Kilivana Admin", "+254700000001", UserRole.ADMIN, adminPassword);
            if (admin.created()) {
                created++;
            }
        } else {
            log.warn("SEED_ADMIN_PASSWORD is not set; the administrator account was not seeded.");
        }

        SeededUser farmer = findOrCreateUser(FARMER_EMAIL, "Wanjiru Mwangi", "+254712345601", UserRole.FARMER, demoPassword);
        SeededUser buyer = findOrCreateUser(BUYER_EMAIL, "Nia Wambui", "+254712345602", UserRole.BUYER, demoPassword);
        SeededUser supplier = findOrCreateUser(SUPPLIER_EMAIL, "Kamau Maina", "+254712345603", UserRole.SUPPLIER, demoPassword);
        SeededUser driver = findOrCreateUser(DRIVER_EMAIL, "Kiprop Saina", "+254712345604", UserRole.DRIVER, demoPassword);
        SeededUser inspector = findOrCreateUser(INSPECTOR_EMAIL, "Grace Achieng", "+254712345605", UserRole.INSPECTOR, demoPassword);
        created += SeededUser.count(farmer, buyer, supplier, driver, inspector);

        User farmerUser = farmer.user();
        User buyerUser = buyer.user();
        User supplierUser = supplier.user();
        User driverUser = driver.user();
        User inspectorUser = inspector.user();

        if (farmerProfileRepository.findByUserId(farmerUser.getId()).isEmpty()) {
            farmerProfileRepository.save(FarmerProfile.builder()
                    .userId(farmerUser.getId())
                    .farmName("Green Valley Farm")
                    .location("Kiambu, Kenya")
                    .farmDetails("Two-acre tea and coffee smallholding on the outskirts of Nairobi")
                    .build());
            created++;
        }

        if (buyerProfileRepository.findByUserId(buyerUser.getId()).isEmpty()) {
            buyerProfileRepository.save(BuyerProfile.builder()
                    .userId(buyerUser.getId())
                    .contactDetails("+254712345602")
                    .build());
            created++;
        }

        if (supplierProfileRepository.findByUserId(supplierUser.getId()).isEmpty()) {
            supplierProfileRepository.save(SupplierProfile.builder()
                    .userId(supplierUser.getId())
                    .businessName("Kilivana Supplies Ltd")
                    .location("Nairobi, Kenya")
                    .businessDetails("Grocery wholesaler serving the Nairobi retail market")
                    .contractEndDate(LocalDate.now().plusYears(1))
                    .build());
            created++;
        }

        if (driverProfileRepository.findByUserId(driverUser.getId()).isEmpty()) {
            driverProfileRepository.save(DriverProfile.builder()
                    .userId(driverUser.getId())
                    .address("Westlands, Nairobi, Kenya")
                    .licenseNumber("KDL-SEED-0001")
                    .licenseExpiryDate(LocalDate.now().plusYears(2))
                    .idType("National ID")
                    .idNumber("29584712")
                    .kycStatus(KycStatus.VERIFIED)
                    .availabilityStatus(DriverStatus.AVAILABLE)
                    .vehicleType(VehicleType.MOTORBIKE)
                    .vehicleNumber("KBD 999C")
                    .vehicleMake("Bajaj")
                    .vehicleCapacityKg(500)
                    .build());
            created++;
        }

        if (inspectorProfileRepository.findByUserId(inspectorUser.getId()).isEmpty()) {
            inspectorProfileRepository.save(InspectorProfile.builder()
                    .userId(inspectorUser.getId())
                    .inspectorDetails("Quality assurance lead with ten years in the tea trade")
                    .specialization("Tea & Coffee")
                    .assignedArea("Kiambu")
                    .status("active")
                    .build());
            created++;
        }

        return created;
    }

    // ---------------------------------------------------------- catalogue

    private int seedCategories() {
        int created = 0;
        // Farmer categories: farm-produced goods
        created += category("Fresh Produce", SellerType.FARMER);
        created += category("Fruits", SellerType.FARMER);
        created += category("Vegetables", SellerType.FARMER);
        created += category("Cereals", SellerType.FARMER);
        created += category("Legumes", SellerType.FARMER);
        created += category("Herbs & Spices", SellerType.FARMER);
        created += category("Tea & Coffee", SellerType.FARMER);
        created += category("Nuts & Seeds", SellerType.FARMER);
        created += category("Dairy & Eggs", SellerType.FARMER);
        created += category("Poultry", SellerType.FARMER);
        created += category("Meat & Livestock", SellerType.FARMER);
        created += category("Honey & Beeswax", SellerType.FARMER);

        // Supplier categories: farming inputs and agricultural supplies
        created += category("Fertilizers", SellerType.SUPPLIER);
        created += category("Seeds & Seedlings", SellerType.SUPPLIER);
        created += category("Animal Feeds", SellerType.SUPPLIER);
        created += category("Farm Equipment", SellerType.SUPPLIER);
        created += category("Agricultural Tools", SellerType.SUPPLIER);
        created += category("Pesticides & Crop Protection", SellerType.SUPPLIER);
        created += category("Irrigation Supplies", SellerType.SUPPLIER);
        created += category("Packaging Materials", SellerType.SUPPLIER);
        return created;
    }

    private int category(String name, SellerType type) {
        if (categoryRepository.existsByName(name)) {
            return 0;
        }
        categoryRepository.save(Category.builder().name(name).type(type).active(true).build());
        return 1;
    }

    private int seedProducts() {
        User farmer = userRepository.findByEmailIgnoreCase(FARMER_EMAIL).orElseThrow();
        User supplier = userRepository.findByEmailIgnoreCase(SUPPLIER_EMAIL).orElseThrow();
        int created = 0;

        // Farmer products
        created += product("Red Delicious Apples", farmer, "Fruits", "Fresh from the orchard, sweet and crisp",
                "kg", money("180.00"), 150, 5);
        created += product("Kale (Sukuma Wiki)", farmer, "Vegetables", "Freshly harvested leafy greens",
                "bunch", money("45.00"), 200, 10);
        created += product("Tomatoes", farmer, "Vegetables", " Vine-ripened, locally grown",
                "kg", money("120.00"), 100, 5);
        created += product("Maize Grain", farmer, "Cereals", "Grade 1 white maize, dried and shelled",
                "bag", money("2500.00"), 50, 2);
        created += product("Cowpeas (Beans)", farmer, "Legumes", "Fresh pigeon peas, shelled",
                "kg", money("180.00"), 80, 5);
        created += product("Fresh Ginger", farmer, "Herbs & Spices", "Organic ginger root, freshly harvested",
                "kg", money("350.00"), 30, 3);
        created += product("Green Tea", farmer, "Tea & Coffee", "Loose leaf green tea from the highlands",
                "kg", money("50.00"), 100, 1);
        created += product("Roasted Coffee Beans", farmer, "Tea & Coffee",
                "Single-origin beans, medium roast", "kg", money("450.00"), 60, 1);
        created += product("Macadamia Nuts", farmer, "Nuts & Seeds",
                "Raw kernel, grade A", "kg", money("600.00"), 40, 1);
        created += product("Hass Avocados", farmer, "Fruits",
                "Fresh from the farm, ready to ripen", "piece", money("35.00"), 200, 50);
        created += product("Fresh Milk", farmer, "Dairy & Eggs",
                "Pasteurized whole milk, morning collection", "litre", money("65.00"), 80, 2);
        created += product("Free-Range Eggs", farmer, "Dairy & Eggs",
                "Large brown eggs, 30pcs crate", "crate", money("320.00"), 40, 1);
        created += product("Broilers (Live)", farmer, "Poultry",
                "5-week old broiler chicken", "piece", money("450.00"), 300, 20);
        created += product("Goat Meat", farmer, "Meat & Livestock",
                "Fresh goat meat, cuts assorted", "kg", money("800.00"), 25, 2);
        created += product("Pure Wildflower Honey", farmer, "Honey & Beeswax",
                "Raw, unfiltered honey from local hives", "jar", money("450.00"), 60, 2);

        // Supplier products
        created += product("DAP Fertilizer", supplier, "Fertilizers",
                "Di-ammonium phosphate, 50kg bag", "bag", money("1800.00"), 200, 1);
        created += product("Nitrogen Fertilizer (CAN)", supplier, "Fertilizers",
                "Calcium ammonium nitrate, 50kg bag", "bag", money("1500.00"), 150, 1);
        created += product("Hybrid Maize Seeds", supplier, "Seeds & Seedlings",
                "High-yield hybrid maize, 1kg packet", "packet", money("450.00"), 500, 5);
        created += product("Tomato Seedlings", supplier, "Seeds & Seedlings",
                "Disease-resistant variety, 50 per pack", "tray", money("800.00"), 300, 2);
        created += product("Broiler Chicken Feed", supplier, "Animal Feeds",
                "Premium starter/finisher feed, 25kg bag", "bag", money("1200.00"), 400, 2);
        created += product("Cattle Feed Block", supplier, "Animal Feeds",
                "Mineral salt lick for dairy cows, 2kg", "piece", money("280.00"), 100, 5);
        created += product("Hand Hoe", supplier, "Agricultural Tools",
                "Ergonomic hand hoe with wooden handle", "piece", money("350.00"), 120, 5);
        created += product("Pruning Shears", supplier, "Agricultural Tools",
                "Stainless steel pruning shears", "pair", money("550.00"), 80, 2);
        created += product("Herbicide (Glyphosate)", supplier, "Pesticides & Crop Protection",
                "Broad-spectrum weed killer, 1L", "bottle", money("450.00"), 250, 2);
        created += product("Insect Netting", supplier, "Pesticides & Crop Protection",
                "Anti-insect garden netting, 10m roll", "roll", money("300.00"), 150, 1);
        created += product("Drip Irrigation Kit", supplier, "Irrigation Supplies",
                "Complete drip irrigation for 0.1 acre", "kit", money("8000.00"), 30, 1);
        created += product("HDPE Grow Bags", supplier, "Packaging Materials",
                "Black grow bags, 15L, pack of 10", "pack", money("1200.00"), 90, 2);

        return created;
    }

    private int product(String name, User seller, String categoryName, String description,
                        String unit, BigDecimal price, int stock, int minimumOrder) {
        if (productRepository.findByNameAndSellerId(name, seller.getId()).isPresent()) {
            return 0;
        }
        Category category = categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new IllegalStateException("Seeded category missing: " + categoryName));
        productRepository.save(Product.builder()
                .sellerId(seller.getId())
                .sellerType(seller.getRole() == UserRole.SUPPLIER ? SellerType.SUPPLIER : SellerType.FARMER)
                .categoryId(category.getId())
                .name(name)
                .description(description)
                .unit(unit)
                .price(price)
                .stockQty(stock)
                .reservedQty(0)
                .soldQty(0)
                .minimumOrderQty(minimumOrder)
                .status(ProductStatus.PENDING_APPROVAL)
                .build());
        return 1;
    }

    // ----------------------------------------------------------- buying

    private int seedAddresses() {
        User buyer = userRepository.findByEmailIgnoreCase(BUYER_EMAIL).orElseThrow();
        int created = 0;
        created += address(buyer, "Home", "Plot 12, Westlands, Nairobi", -1.26, 36.81);
        created += address(buyer, "Work", "Mombasa Road, Industrial Area, Nairobi", -1.29, 36.82);
        return created;
    }

    private int address(User buyer, String label, String text, double latitude, double longitude) {
        boolean exists = addressRepository.findByUserId(buyer.getId()).stream()
                .anyMatch(address -> label.equals(address.getLabel()));
        if (exists) {
            return 0;
        }
        addressRepository.save(Address.builder()
                .userId(buyer.getId())
                .label(label)
                .addressText(text)
                .latitude(latitude)
                .longitude(longitude)
                .build());
        return 1;
    }

    private int seedOrdersAndPayments() {
        User buyer = userRepository.findByEmailIgnoreCase(BUYER_EMAIL).orElseThrow();
        User farmer = userRepository.findByEmailIgnoreCase(FARMER_EMAIL).orElseThrow();
        User supplier = userRepository.findByEmailIgnoreCase(SUPPLIER_EMAIL).orElseThrow();

        Address home = addressByLabel(buyer, "Home");
        Address work = addressByLabel(buyer, "Work");

        Product tea = productRepository.findByNameAndSellerId("Green Tea", farmer.getId()).orElseThrow();
        Product flour = productRepository.findByNameAndSellerId("Maize Flour (Unga)", supplier.getId()).orElseThrow();
        Product avocados = productRepository.findByNameAndSellerId("Hass Avocados", farmer.getId()).orElseThrow();
        Product milk = productRepository.findByNameAndSellerId("Fresh Milk", farmer.getId()).orElseThrow();
        Product coffee = productRepository.findByNameAndSellerId("Roasted Coffee Beans", farmer.getId()).orElseThrow();

        int created = 0;

        // Order one: a basket of farm and pantry goods, later disputed.
        created += order(ORDER_ONE, buyer, home, money("25.00"),
                line(tea, 2), line(flour, 1));
        created += initiatedPayment(PAYMENT_ONE, ORDER_ONE, PaymentMethod.MPESA, money("280.00"));

        // Order two: fresh produce, confirmed and out for delivery.
        created += order(ORDER_TWO, buyer, work, money("30.00"),
                line(avocados, 3), line(milk, 2));
        created += initiatedPayment(PAYMENT_TWO, ORDER_TWO, PaymentMethod.MPESA, money("235.00"));

        // Order three: a single coffee order on its way.
        created += order(ORDER_THREE, buyer, home, money("35.00"),
                line(coffee, 1));
        created += initiatedPayment(PAYMENT_THREE, ORDER_THREE, PaymentMethod.BANK, money("450.00"));

        return created;
    }

    /**
     * Writes an order with the given reference, its line items and its
     * opening timeline event, and reserves the stock the lines buy. The
     * figures are computed from the items, exactly as the placement
     * endpoint computes them.
     */
    private int order(String code, User buyer, Address address, BigDecimal deliveryFee,
                      LineItem... lines) {
        if (orderRepository.findByCode(code).isPresent()) {
            return 0;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (LineItem line : lines) {
            Product product = line.product;
            subtotal = subtotal.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(line.quantity)));
            // Reserve the stock now, the way a live order would.
            product.setReservedQty(nullToZero(product.getReservedQty()) + line.quantity);
            productRepository.save(product);
            items.add(OrderItem.builder()
                    .productId(product.getId())
                    .sellerId(product.getSellerId())
                    .productName(product.getName())
                    .unit(product.getUnit())
                    .quantity(line.quantity)
                    .unitPrice(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(line.quantity)))
                    .build());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .code(code)
                .buyerId(buyer.getId())
                .status(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .addressId(address.getId())
                .build();
        Order saved = orderRepository.save(order);
        items.forEach(item -> {
            item.setOrderId(saved.getId());
            orderItemRepository.save(item);
        });
        orderEventRepository.save(OrderEvent.builder()
                .orderId(saved.getId())
                .status(OrderStatus.PLACED)
                .note("Order placed")
                .build());
        // The order, its line items, and the opening timeline event.
        return 1 + items.size() + 1;
    }

    /**
     * An <em>initiated</em> payment: the buyer has started an M-Pesa or
     * bank payment, but the provider has not confirmed it, so nothing
     * here looks like a completed transaction.
     */
    private int initiatedPayment(String reference, String orderCode, PaymentMethod method, BigDecimal amount) {
        if (paymentRepository.findByReference(reference).isPresent()) {
            return 0;
        }
        Order order = orderRepository.findByCode(orderCode).orElseThrow();
        paymentRepository.save(Payment.builder()
                .orderId(order.getId())
                .method(method)
                .reference(reference)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build());
        return 1;
    }

    // --------------------------------------------------------- logistics

    /**
     * One delivery job for the confirmed order, assigned to the seeded
     * driver, with the tracking events a job accumulates on its way out.
     */
    private int seedLogistics() {
        User buyer = userRepository.findByEmailIgnoreCase(BUYER_EMAIL).orElseThrow();
        User driver = userRepository.findByEmailIgnoreCase(DRIVER_EMAIL).orElseThrow();
        Order order = orderRepository.findByCode(ORDER_TWO).orElseThrow();

        if (!logisticsService.getJobsByOrder(order.getId()).isEmpty()) {
            return 0;
        }
        Address work = addressByLabel(buyer, "Work");
        Address home = addressByLabel(buyer, "Home");

        LogisticsJob job = logisticsService.createJob(LogisticsJob.builder()
                .orderId(order.getId())
                .pickupAddress("Green Valley Farm, Kiambu, Kenya")
                .pickupLatitude(-1.17)
                .pickupLongitude(36.83)
                .destinationAddress(work.getAddressText())
                .destinationLatitude(work.getLatitude())
                .destinationLongitude(work.getLongitude())
                .cargoDescription("Hass Avocados, Fresh Milk")
                .quantity(5)
                .distanceKm(24.5)
                .estimatedMinutes(45)
                .scheduledPickupAt(LocalDateTime.now().plusHours(1))
                .scheduledDropoffAt(LocalDateTime.now().plusHours(2))
                .build());

        logisticsService.assignDriver(job.getId(), driver.getId());

        trackingEventRepository.save(TrackingEvent.builder()
                .logisticsJobId(job.getId())
                .status(DeliveryStatus.ASSIGNED)
                .driverId(driver.getId())
                .note("Driver assigned and notified")
                .recordedAt(LocalDateTime.now())
                .build());
        trackingEventRepository.save(TrackingEvent.builder()
                .logisticsJobId(job.getId())
                .status(DeliveryStatus.EN_ROUTE_TO_PICKUP)
                .driverId(driver.getId())
                .latitude(-1.26)
                .longitude(36.81)
                .note("Driver on the way to Green Valley Farm")
                .recordedAt(LocalDateTime.now())
                .build());

        // The disputed order keeps its address visible for the demo, and
        // the third order sits in transit with a tracking point.
        Order third = orderRepository.findByCode(ORDER_THREE).orElseThrow();
        third.setStatus(OrderStatus.IN_TRANSIT);
        orderRepository.save(third);
        orderEventRepository.save(OrderEvent.builder()
                .orderId(third.getId())
                .status(OrderStatus.IN_TRANSIT)
                .note("Order handed to the courier")
                .build());
        Order confirmed = orderRepository.findByCode(ORDER_TWO).orElseThrow();
        confirmed.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(confirmed);
        orderEventRepository.save(OrderEvent.builder()
                .orderId(confirmed.getId())
                .status(OrderStatus.CONFIRMED)
                .note("Seller confirmed the order")
                .build());
        // The job, its two tracking events, and the two timeline events.
        return 1 + 2 + 2;
    }

    // ----------------------------------------------------------- disputes

    /**
     * One open dispute against the first order. Its payment is still
     * awaiting the provider, so raising the dispute marks the order
     * disputed without moving any money.
     */
    private int seedDispute() {
        Order order = orderRepository.findByCode(ORDER_ONE).orElseThrow();
        User buyer = userRepository.findByEmailIgnoreCase(BUYER_EMAIL).orElseThrow();
        if (!disputeRepository.findByOrderId(order.getId()).isEmpty()) {
            return 0;
        }
        disputeService.createDispute(DisputeRequest.builder()
                .orderId(order.getId())
                .raisedBy(buyer.getId())
                .reason(DISPUTE_REASON)
                .description("The bag of maize flour arrived torn")
                .status(DisputeStatus.OPEN)
                .build());
        return 1;
    }

    // ------------------------------------------------------------ helpers

    private Address addressByLabel(User buyer, String label) {
        return addressRepository.findByUserId(buyer.getId()).stream()
                .filter(address -> label.equals(address.getLabel()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeded address missing: " + label));
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private record LineItem(Product product, int quantity) {
    }

    private static LineItem line(Product product, int quantity) {
        return new LineItem(product, quantity);
    }
}
