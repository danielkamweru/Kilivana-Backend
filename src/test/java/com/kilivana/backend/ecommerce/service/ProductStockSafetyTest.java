package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductStockSafetyTest {
    final ProductRepository products = mock(ProductRepository.class);
    final ProductService service = new ProductService(products, mock(ProductImageRepository.class),
            mock(CategoryRepository.class), mock(ImageStorage.class), mock(UserService.class),
            mock(com.kilivana.backend.security.ProductVisibility.class));
    @Test void editingCannotEraseReservations() {
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(Product.builder().id(1L).stockQty(10).reservedQty(8).build()));
        assertThrows(BadRequestException.class, () -> service.updateProduct(1L, ProductRequest.builder().stockQty(7).build()));
        verify(products, never()).save(any());
    }
    @Test void validEditPreservesReservationsAndPendingModeration() {
        var product = Product.builder().id(1L).stockQty(10).reservedQty(8).status(ProductStatus.PENDING_APPROVAL).build();
        when(products.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(products.save(any())).thenAnswer(i -> i.getArgument(0));
        service.updateProduct(1L, ProductRequest.builder().stockQty(12).build());
        assertEquals(8, product.getReservedQty());
        assertEquals(ProductStatus.PENDING_APPROVAL, product.getStatus());
        assertEquals(12, product.getStockQty());
    }
}
