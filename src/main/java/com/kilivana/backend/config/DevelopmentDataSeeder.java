package com.kilivana.backend.config;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Adds buyer-facing demo products on top of {@link SeedDataRunner}'s seed.
 *
 * <p>Gated behind {@code DEV_SEED_ENABLED} so it only runs where the full demo
 * dataset is wanted, and upserts by name so re-running it is safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "DEV_SEED_ENABLED", havingValue = "true")
public class DevelopmentDataSeeder {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Value("${DEV_FARMER_EMAIL:farmer@kilivana.demo}")
    private String farmerEmail;

    @Value("${DEV_SUPPLIER_EMAIL:supplier@kilivana.demo}")
    private String supplierEmail;

    /**
     * Inserts buyer-facing demo products on top of {@link SeedDataRunner}'s
     * data. Runs only behind the {@code DEV_SEED_ENABLED} flag and upserts by
     * name so it is safe to re-run.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDevelopmentData() {
        log.info("Development seed enabled — inserting demo buyer-side products");

        User farmer = userRepository.findByEmailIgnoreCase(farmerEmail)
                .orElseThrow(() -> new IllegalStateException("Farmer not found: " + farmerEmail));
        User supplier = userRepository.findByEmailIgnoreCase(supplierEmail)
                .orElseThrow(() -> new IllegalStateException("Supplier not found: " + supplierEmail));

        Category tomatoes = upsertCategory("Fresh Produce", SellerType.FARMER);
        Category sweetPotatoes = upsertCategory("Root Crops", SellerType.FARMER);
        Category fertilizer = upsertCategory("Fertilizers", SellerType.SUPPLIER);
        Category maizeSeeds = upsertCategory("Seeds & Seedlings", SellerType.SUPPLIER);

        upsertProduct("Tomatoes", farmer, tomatoes, SellerType.FARMER,
                "Vine-ripened, locally grown", "kg", new BigDecimal("120.00"), 100, 5);
        upsertProduct("Sweet Potatoes", farmer, sweetPotatoes, SellerType.FARMER,
                "Orange-fleshed, rich in beta-carotene", "kg", new BigDecimal("90.00"), 80, 5);
        upsertProduct("DAP Fertilizer", supplier, fertilizer, SellerType.SUPPLIER,
                "Di-ammonium phosphate, 50kg bag", "bag", new BigDecimal("1800.00"), 200, 1);
        upsertProduct("Hybrid Maize Seeds", supplier, maizeSeeds, SellerType.SUPPLIER,
                "High-yield hybrid maize, 1kg packet", "packet", new BigDecimal("450.00"), 500, 5);

        log.info("Development seed complete");
    }

    private Category upsertCategory(String name, SellerType type) {
        return categoryRepository.findByName(name)
                .map(existing -> {
                    if (!existing.getType().equals(type)) {
                        existing.setType(type);
                    }
                    existing.setActive(true);
                    return categoryRepository.save(existing);
                })
                .orElseGet(() -> categoryRepository.save(
                        Category.builder().name(name).type(type).active(true).build()));
    }

    private void upsertProduct(String name, User seller, Category category, SellerType sellerType,
                               String description, String unit, BigDecimal price, int stock, int minOrder) {
        if (productRepository.findByNameAndSellerId(name, seller.getId()).isPresent()) {
            return;
        }
        productRepository.save(Product.builder()
                .sellerId(seller.getId())
                .sellerType(sellerType)
                .categoryId(category.getId())
                .name(name)
                .description(description)
                .unit(unit)
                .price(price)
                .stockQty(stock)
                .reservedQty(0)
                .soldQty(0)
                .minimumOrderQty(minOrder)
                .status(ProductStatus.ACTIVE)
                .build());
        log.info("Seeded product: {} ({} - {})", name, sellerType, category.getName());
    }
}
