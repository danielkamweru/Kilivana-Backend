package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import com.kilivana.backend.ecommerce.repository.ProductImageRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.security.ProductVisibility;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductCreationTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserService users = mock(UserService.class);
    private final ProductService service = new ProductService(
            products,
            mock(ProductImageRepository.class),
            mock(CategoryRepository.class),
            mock(ImageStorage.class),
            users,
            mock(ProductVisibility.class));

    @Test
    void createsProductsPendingApproval() {
        when(users.getUserById(4L)).thenReturn(UserResponse.builder().id(4L).role(UserRole.FARMER).build());
        when(products.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(42L);
            return product;
        });
        ProductRequest request = ProductRequest.builder()
                .name("Fresh cabbages")
                .description("Fresh produce")
                .unit("kg")
                .price(BigDecimal.TEN)
                .stockQty(10)
                .minimumOrderQty(1)
                .categoryId(1L)
                .build();

        var response = service.createProduct(request, 4L);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
        verify(products).save(org.mockito.ArgumentMatchers.argThat(
                product -> product.getStatus() == ProductStatus.PENDING_APPROVAL));
    }
}
