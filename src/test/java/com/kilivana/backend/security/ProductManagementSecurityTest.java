package com.kilivana.backend.security;

import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.exception.*;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.*;
import com.kilivana.backend.ecommerce.repository.*;
import com.kilivana.backend.ecommerce.service.ProductService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real method-security proxies with isolated mocks; no datasource or network. */
@SpringJUnitConfig(ServiceAuthorizationTest.Config.class)
class ProductManagementSecurityTest {
    @Autowired ProductService service;
    @Autowired ProductRepository products;
    @Autowired ProductImageRepository images;
    @Autowired UserRepository users;
    @Autowired ImageStorage storage;
    Product product;
    @BeforeEach void setup() {
        reset(products, images, users, storage);
        actor(4L, UserRole.FARMER);
        product = Product.builder().id(42L).sellerId(4L).sellerType(SellerType.FARMER)
                .status(ProductStatus.ACTIVE).name("Tomatoes").description("Fresh")
                .price(BigDecimal.TEN).stockQty(12).reservedQty(0).soldQty(6).build();
        when(products.findById(42L)).thenReturn(Optional.of(product));
        when(products.findByIdForUpdate(42L)).thenReturn(Optional.of(product));
        when(products.findBySellerId(4L)).thenReturn(List.of(product));
        when(products.save(any())).thenAnswer(i -> i.getArgument(0));
        when(products.findByStatus(any())).thenAnswer(i -> product.getStatus() == i.getArgument(0) ? List.of(product) : List.of());
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void actor(Long id, UserRole role) {
        when(users.findById(id)).thenReturn(Optional.of(User.builder().id(id).name("Fixture")
                .role(role).status(UserStatus.ACTIVE).emailVerified(true).build()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
    }
    @Test void unlistPreservesDataAndOwnerAccessButHidesPublicAndRelistNeedsApproval() {
        var image = ProductImage.builder().id(2L).productId(42L).url("/image").build();
        when(images.findByProductIdOrderBySortOrderAsc(42L)).thenReturn(List.of(image));
        assertEquals(1, service.getAllProducts().size());
        var response = service.updateSellerProductStatus(42L, ProductStatus.UNLISTED);
        assertEquals(ProductStatus.UNLISTED, response.getStatus());
        assertEquals(BigDecimal.TEN, response.getPrice());
        assertEquals("Fresh", response.getDescription());
        assertEquals("/image", response.getImages().get(0).getUrl());
        assertTrue(service.getAllProducts().isEmpty());
        assertEquals(1, service.getProductsBySeller(4L).size());
        assertEquals(42L, service.getProductById(42L).getId());
        actor(5L, UserRole.FARMER);
        assertTrue(service.getProductsBySeller(4L).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProductById(42L));
        actor(4L, UserRole.FARMER);
        assertThrows(BadRequestException.class, () -> service.updateSellerProductStatus(42L, ProductStatus.ACTIVE));
        assertEquals(ProductStatus.PENDING_APPROVAL, service.updateSellerProductStatus(42L, ProductStatus.PENDING_APPROVAL).getStatus());
        assertTrue(service.getAllProducts().isEmpty());
        actor(99L, UserRole.ADMIN);
        assertEquals(ProductStatus.ACTIVE, service.updateProductStatus(42L, ProductStatus.ACTIVE).getStatus());
    }
    @ParameterizedTest @EnumSource(value=ProductStatus.class, names={"PENDING_APPROVAL","REJECTED","SUSPENDED","ARCHIVED"})
    void cannotUnlistNonpublished(ProductStatus state) {
        product.setStatus(state);
        assertThrows(BadRequestException.class, () -> service.updateSellerProductStatus(42L, ProductStatus.UNLISTED));
        verify(products, never()).save(any());
    }
    @ParameterizedTest @EnumSource(value=ProductStatus.class, names={"ACTIVE","PENDING_APPROVAL","REJECTED","SUSPENDED","UNLISTED","ARCHIVED"})
    void deletionFromAnyNonDeletedStatusRetainsHistoricalRowAndCleansImages(ProductStatus status) throws Exception {
        product.setStatus(status);
        var image=ProductImage.builder().id(2L).productId(42L).publicId("stored-2").build();
        when(images.findByProductId(42L)).thenReturn(List.of(image));
        service.deleteProduct(42L);
        assertEquals(ProductStatus.DELETED, product.getStatus());
        assertEquals(6, product.getSoldQty());
        assertEquals("Tomatoes", product.getName());
        verify(products, never()).deleteById(any());
        verify(products, never()).delete(any());
        verify(storage).deleteImage("stored-2");
        verify(images).deleteAll(List.of(image));
        assertTrue(service.getProductsBySeller(4L).isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteProduct(42L));
        assertThrows(ResourceNotFoundException.class, () -> service.getProductById(42L));
        actor(99L, UserRole.ADMIN);
        assertThrows(ResourceNotFoundException.class, () -> service.updateProductStatus(42L, ProductStatus.ACTIVE));
    }
    @Test void reservedProductsCannotBeDeleted() {
        product.setReservedQty(1);
        assertThrows(BadRequestException.class, () -> service.deleteProduct(42L));
        verifyNoInteractions(storage);
        verify(products, never()).save(any());
    }
    @ParameterizedTest @EnumSource(value=UserRole.class, names={"BUYER","FARMER","SUPPLIER","INSPECTOR","DRIVER"})
    void unrelatedActorsCannotChangeLifecycleOrAnyImages(UserRole role) {
        actor(5L, role);
        assertThrows(AccessDeniedException.class, () -> service.updateSellerProductStatus(42L, ProductStatus.UNLISTED));
        assertThrows(AccessDeniedException.class, () -> service.deleteProduct(42L));
        assertThrows(AccessDeniedException.class, () -> service.updateProduct(42L, new ProductRequest()));
        assertThrows(AccessDeniedException.class, () -> service.uploadProductImage(42L, null, null, null));
        assertThrows(AccessDeniedException.class, () -> service.deleteProductImage(42L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.updateProductImage(42L, 2L, null, null, null));
        assertThrows(AccessDeniedException.class, () -> service.setPrimaryImage(42L, 2L));
        assertThrows(AccessDeniedException.class, () -> service.reorderImages(42L, List.of(2L)));
        verify(products, never()).findByIdForUpdate(any());
        verifyNoInteractions(images, storage);
    }
    @Test void evenMatchingIdMustHaveSellerRole() {
        actor(4L, UserRole.INSPECTOR);
        assertThrows(AccessDeniedException.class, () -> service.ensureProductOwnership(42L, 4L));
        assertThrows(AccessDeniedException.class, () -> service.deleteProduct(42L));
    }
    @Test void supplierAndAdminCanUseOwnerWorkflow() throws Exception {
        actor(4L, UserRole.SUPPLIER);
        service.updateSellerProductStatus(42L, ProductStatus.UNLISTED);
        service.deleteProduct(42L);
        assertEquals(ProductStatus.DELETED, product.getStatus());

        product.setStatus(ProductStatus.ACTIVE);
        actor(99L, UserRole.ADMIN);
        service.updateSellerProductStatus(42L, ProductStatus.ARCHIVED);
        service.deleteProduct(42L);
        assertEquals(ProductStatus.DELETED, product.getStatus());
    }
    @Test void adminModerationSuspensionCannotBeClearedBySeller() {
        product.setStatus(ProductStatus.SUSPENDED);
        for (var state : List.of(ProductStatus.ACTIVE, ProductStatus.UNLISTED, ProductStatus.PENDING_APPROVAL, ProductStatus.ARCHIVED))
            assertThrows(BadRequestException.class, () -> service.updateSellerProductStatus(42L, state));
    }
}
