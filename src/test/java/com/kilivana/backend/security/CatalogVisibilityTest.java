package com.kilivana.backend.security;

import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.controller.StoredImageController;
import com.kilivana.backend.common.entity.StoredImage;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.repository.StoredImageRepository;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.config.SecurityConfig;
import com.kilivana.backend.ecommerce.controller.*;
import com.kilivana.backend.ecommerce.entity.*;
import com.kilivana.backend.ecommerce.repository.*;
import com.kilivana.backend.ecommerce.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real controllers, visibility service and filter chain, with no datasource or live writes. */
@WebMvcTest({ProductController.class, CatalogCompatibilityController.class,
        StoredImageController.class, CategoryController.class})
@Import({SecurityConfig.class, ProductService.class, ProductVisibility.class})
class CatalogVisibilityTest {
    @Autowired MockMvc mvc;
    @MockBean ProductRepository products;
    @MockBean ProductImageRepository images;
    @MockBean CategoryRepository categories;
    @MockBean StoredImageRepository stored;
    @MockBean ImageStorage storage;
    @MockBean UserService users;
    @MockBean CategoryService categoryService;
    @MockBean ResourceAuthorization authorization;
    @MockBean AccountAccessValidator accounts;
    Product pending;

    static RequestPostProcessor actor(long id, String role) {
        return authentication(new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @BeforeEach void setup() {
        pending = Product.builder().id(10L).sellerId(1L).stockQty(5)
                .status(ProductStatus.PENDING_APPROVAL).build();
        var approved = Product.builder().id(11L).sellerId(1L).stockQty(5)
                .status(ProductStatus.ACTIVE).build();
        when(products.findById(10L)).thenReturn(Optional.of(pending));
        when(products.findById(11L)).thenReturn(Optional.of(approved));
        when(products.findBySellerId(1L)).thenReturn(List.of(pending, approved));
        when(products.findByStatus(ProductStatus.ACTIVE)).thenReturn(List.of(approved));
        when(products.findByCategoryId(3L)).thenReturn(List.of(pending, approved));
        when(images.findByProductIdOrderBySortOrderAsc(10L)).thenReturn(List.of(
                ProductImage.builder().id(20L).productId(10L).publicId("30").url("/api/v1/images/30").build()));
        when(images.findAllByPublicId("30")).thenReturn(List.of(
                ProductImage.builder().id(20L).productId(10L).publicId("30").build()));
        when(stored.findById(30L)).thenReturn(Optional.of(StoredImage.builder().id(30L)
                .folder("kilivana_products_10").contentType("image/png").byteLength(3L).data(new byte[]{1,2,3}).build()));
        when(authorization.seller(any())).thenAnswer(i -> {
            var a = SecurityContextHolder.getContext().getAuthentication();
            if (a == null || !(a.getPrincipal() instanceof Long id)) return false;
            return a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN")) ||
                    (id.equals(i.getArgument(0)) && a.getAuthorities().stream().anyMatch(r ->
                            r.getAuthority().equals("ROLE_FARMER") || r.getAuthority().equals("ROLE_SUPPLIER")));
        });
    }

    @ParameterizedTest @ValueSource(strings={"/api/v1/products/10", "/api/v1/products/10/images", "/api/v1/images/30"})
    void anonymousCannotReadPendingDetailMetadataOrBytes(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings={"BUYER", "FARMER", "INSPECTOR"})
    void unrelatedRolesCannotReadPending(String role) throws Exception {
        for (String route : List.of("/api/v1/products/10", "/api/v1/products/10/images", "/api/v1/images/30"))
            mvc.perform(get(route).with(actor(2, role))).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings={"FARMER", "ADMIN"})
    void ownerAndAdminRetainPendingReadContracts(String role) throws Exception {
        long id = role.equals("ADMIN") ? 9 : 1;
        mvc.perform(get("/api/v1/products/10").with(actor(id,role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending"));
        mvc.perform(get("/api/v1/products/10/images").with(actor(id,role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(20));
        mvc.perform(get("/api/v1/images/30").with(actor(id,role)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[]{1,2,3}));
    }
    @Test void publicListsOnlyExposeApproved() throws Exception {
        for (String path : List.of("/api/v1/products", "/api/v1/products/category/3", "/api/v1/products/seller/1"))
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].id").value(11));
        mvc.perform(get("/api/v1/products/11")).andExpect(status().isOk());
    }
    @Test void sellerAliasesRespectIdentityRatherThanSuppliedSellerId() throws Exception {
        for (String path : List.of("/api/v1/products/seller/1", "/api/v1/sellers/1/products")) {
            mvc.perform(get(path).with(actor(2,"BUYER"))).andExpect(jsonPath("$.data.length()").value(1));
            mvc.perform(get(path).with(actor(1,"FARMER"))).andExpect(jsonPath("$.data.length()").value(2));
            mvc.perform(get(path).with(actor(9,"ADMIN"))).andExpect(jsonPath("$.data.length()").value(2));
        }
    }
    @ParameterizedTest @ValueSource(strings={"REJECTED", "SUSPENDED"})
    void otherUnapprovedStatusesArePrivate(String status) throws Exception {
        pending.setStatus(ProductStatus.valueOf(status));
        mvc.perform(get("/api/v1/products/10")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/products/10/images").with(actor(2,"BUYER"))).andExpect(status().isNotFound());
    }
    @Test void approvedImageBytesArePublicButNotSharedCached() throws Exception {
        pending.setStatus(ProductStatus.ACTIVE);
        mvc.perform(get("/api/v1/images/30")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }
    @Test void approvalChangesTakeEffectWithoutChangingRoutes() throws Exception {
        mvc.perform(get("/api/v1/products/10")).andExpect(status().isNotFound());
        pending.setStatus(ProductStatus.ACTIVE); // In-memory fixture only, no approval/data write.
        mvc.perform(get("/api/v1/products/10")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/10/images")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/images/30")).andExpect(status().isOk());
        pending.setStatus(ProductStatus.SUSPENDED);
        mvc.perform(get("/api/v1/images/30")).andExpect(status().isNotFound());
    }
    @Test void orphanProductImagesFailClosed() throws Exception {
        when(images.findAllByPublicId("30")).thenReturn(List.of());
        mvc.perform(get("/api/v1/images/30")).andExpect(status().isNotFound());
    }
    @Test void categoryWritesRequireAdminWhileReadsRemainPublic() throws Exception {
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        var requests = List.of(post("/api/v1/categories"), put("/api/v1/categories/3"),
                put("/api/v1/categories/3/status"), delete("/api/v1/categories/3"));
        for (var request : requests) {
            mvc.perform(request.contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(request.with(actor(1,"FARMER"))).andExpect(status().isForbidden());
            mvc.perform(request.with(actor(2,"BUYER"))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/categories").with(actor(9,"ADMIN"))
                .contentType("application/json").content("{\"name\":\"Test\",\"type\":\"FARMER\"}"))
                .andExpect(status().isCreated());
        verify(categoryService).createCategory(any());
    }
    @Test void otherPublicGetPathsDoNotPermitAnonymousWrites() throws Exception {
        mvc.perform(post("/api/v1/products").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/images/30")).andExpect(status().isUnauthorized());
        mvc.perform(head("/api/v1/products/10")).andExpect(status().isNotFound());
    }
    @Test void categoryOptionsRemainAvailableWithoutCredentials() throws Exception {
        mvc.perform(options("/api/v1/categories")).andExpect(status().isOk());
        mvc.perform(options("/api/v1/categories/3")).andExpect(status().isOk());
    }
}
