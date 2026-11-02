package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.admin.entity.SupplierWarehouse;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import com.kilivana.backend.ecommerce.repository.ProductImageRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.admin.repository.SupplierWarehouseRepository;
import com.kilivana.backend.security.ProductVisibility;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductCreationTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserService users = mock(UserService.class);
    private final FarmRepository farms = mock(FarmRepository.class);
    private final SupplierWarehouseRepository warehouses = mock(SupplierWarehouseRepository.class);
    private final ProductService service = new ProductService(
            products,
            mock(ProductImageRepository.class),
            mock(CategoryRepository.class),
            mock(ImageStorage.class),
            users,
            mock(ProductVisibility.class),
            farms,
            warehouses);

    @Test
    void createsProductsPendingApprovalWithFarmerDispatchLocation() {
        when(users.getUserById(4L)).thenReturn(UserResponse.builder().id(4L).role(UserRole.FARMER).build());
        when(farms.findById(3L)).thenReturn(Optional.of(Farm.builder()
                .id(3L)
                .farmerId(4L)
                .status(FarmStatus.ACTIVE)
                .address("Farm road")
                .latitude(-1.2)
                .longitude(36.8)
                .build()));
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
                .farmId(3L)
                .build();

        var response = service.createProduct(request, 4L);

        assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
        verify(products).save(org.mockito.ArgumentMatchers.argThat(
                product -> product.getStatus() == ProductStatus.PENDING_APPROVAL
                        && product.getFarmId().equals(3L)));
    }

    @Test
    void rejectsFarmerProductWithoutDispatchFarm() {
        when(users.getUserById(4L)).thenReturn(UserResponse.builder().id(4L).role(UserRole.FARMER).build());

        assertThrows(BadRequestException.class, () -> service.createProduct(productRequest(), 4L));

        verify(products, never()).save(any(Product.class));
    }

    @Test
    void createsSupplierProductWithActiveWarehouse() {
        when(users.getUserById(5L)).thenReturn(UserResponse.builder().id(5L).role(UserRole.SUPPLIER).build());
        when(warehouses.findByIdAndUserId(8L, 5L)).thenReturn(Optional.of(SupplierWarehouse.builder()
                .id(8L)
                .userId(5L)
                .active(true)
                .address("Warehouse road")
                .latitude(-1.3)
                .longitude(36.9)
                .build()));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductRequest request = productRequest();
        request.setWarehouseId(8L);

        service.createProduct(request, 5L);

        verify(products).save(org.mockito.ArgumentMatchers.argThat(
                product -> product.getSupplierWarehouseId().equals(8L)));
    }

    @Test
    void rejectsSupplierProductWithoutDispatchWarehouse() {
        when(users.getUserById(5L)).thenReturn(UserResponse.builder().id(5L).role(UserRole.SUPPLIER).build());

        assertThrows(BadRequestException.class, () -> service.createProduct(productRequest(), 5L));

        verify(products, never()).save(any(Product.class));
    }

    private static ProductRequest productRequest() {
        return ProductRequest.builder()
                .name("Fresh cabbages")
                .description("Fresh produce")
                .unit("kg")
                .price(BigDecimal.TEN)
                .stockQty(10)
                .minimumOrderQty(1)
                .categoryId(1L)
                .build();
    }
}
