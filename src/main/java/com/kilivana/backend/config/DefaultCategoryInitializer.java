package com.kilivana.backend.config;

import com.kilivana.backend.common.enums.SellerType;
import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Adds the catalogue taxonomy required by new installations.
 *
 * <p>This is intentionally independent of {@code SEED_DATA}: categories are required
 * application configuration, while the demo users, orders and payments are not. Existing
 * categories are never changed, so an administrator can deactivate, rename, or otherwise
 * manage a category after the initial insertion.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.catalog.seed-default-categories", havingValue = "true", matchIfMissing = true)
public class DefaultCategoryInitializer {

    private static final List<CategoryTemplate> DEFAULT_CATEGORIES = List.of(
            new CategoryTemplate("Fruits", SellerType.FARMER),
            new CategoryTemplate("Vegetables", SellerType.FARMER),
            new CategoryTemplate("Cereals", SellerType.FARMER),
            new CategoryTemplate("Legumes", SellerType.FARMER),
            new CategoryTemplate("Herbs & Spices", SellerType.FARMER),
            new CategoryTemplate("Tea & Coffee", SellerType.FARMER),
            new CategoryTemplate("Nuts & Seeds", SellerType.FARMER),
            new CategoryTemplate("Dairy & Eggs", SellerType.FARMER),
            new CategoryTemplate("Poultry", SellerType.FARMER),
            new CategoryTemplate("Meat & Livestock", SellerType.FARMER),
            new CategoryTemplate("Honey & Beeswax", SellerType.FARMER),
            new CategoryTemplate("Fertilizers", SellerType.SUPPLIER),
            new CategoryTemplate("Seeds & Seedlings", SellerType.SUPPLIER),
            new CategoryTemplate("Animal Feeds", SellerType.SUPPLIER),
            new CategoryTemplate("Farm Equipment", SellerType.SUPPLIER),
            new CategoryTemplate("Agricultural Tools", SellerType.SUPPLIER),
            new CategoryTemplate("Pesticides & Crop Protection", SellerType.SUPPLIER),
            new CategoryTemplate("Irrigation Supplies", SellerType.SUPPLIER),
            new CategoryTemplate("Packaging Materials", SellerType.SUPPLIER)
    );

    private final CategoryRepository categoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void addMissingDefaultCategories() {
        int created = 0;
        for (CategoryTemplate template : DEFAULT_CATEGORIES) {
            if (categoryRepository.existsByName(template.name())) {
                continue;
            }
            categoryRepository.save(Category.builder()
                    .name(template.name())
                    .type(template.type())
                    .active(true)
                    .build());
            created++;
        }
        log.info("Default category initialization complete: {} category record(s) created.", created);
    }

    private record CategoryTemplate(String name, SellerType type) {
    }
}
