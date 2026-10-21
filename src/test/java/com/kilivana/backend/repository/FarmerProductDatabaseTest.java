package com.kilivana.backend.repository;

import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.common.enums.*;
import com.kilivana.backend.common.service.*;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.entity.*;
import com.kilivana.backend.ecommerce.repository.*;
import com.kilivana.backend.ecommerce.service.ProductService;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.security.ProductVisibility;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="KILIVANA_RUN_API_AUDIT_DB_TESTS",matches="true")
@EnabledIfEnvironmentVariable(named="SPRING_DATASOURCE_URL",matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/kilivana_audit")
@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
        "app.seed.enabled=false","app.storage.provider=database"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductService.class,DatabaseImageStorage.class,DatabaseImageStorageProvider.class})
class FarmerProductDatabaseTest {
    @Autowired ProductRepository products;
    @Autowired ProductImageRepository images;
    @Autowired FarmRepository farms;
    @Autowired com.kilivana.backend.admin.repository.UserRepository farmerAccounts;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired PaymentRepository payments;
    @Autowired ProductService service;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockBean UserService users;
    @MockBean ProductVisibility visibility;
    Product product(ProductStatus status) {
        return products.saveAndFlush(Product.builder().sellerId(4L).sellerType(SellerType.FARMER).categoryId(1L)
            .name("Audit Tomatoes").description("Original").unit("kg").price(BigDecimal.TEN).stockQty(10)
            .reservedQty(0).soldQty(2).minimumOrderQty(1).status(status).build());
    }
    MockMultipartFile png() { return new MockMultipartFile("image","photo.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,0,0,0,0}); }
    @Test void publicQueriesExcludeUnlistedAndArchivedWhileOwnerCanRead() {
        var p=product(ProductStatus.ACTIVE);
        assertEquals(1,products.searchProducts("Audit Tomatoes",null,null,PageRequest.of(0,10)).getTotalElements());
        service.updateSellerProductStatus(p.getId(),ProductStatus.UNLISTED); em.flush(); em.clear();
        assertTrue(products.searchProducts("Audit Tomatoes",null,null,PageRequest.of(0,10)).isEmpty());
        assertTrue(service.getAllProducts().isEmpty());
        assertTrue(service.getProductsByCategory(1L).isEmpty());
        when(visibility.canReviewSeller(4L)).thenReturn(true);
        assertEquals(1,service.getProductsBySeller(4L).size());
        service.updateSellerProductStatus(p.getId(),ProductStatus.ARCHIVED); em.flush();
        assertTrue(products.searchProducts(null,null,null,PageRequest.of(0,10)).isEmpty());
        service.updateSellerProductStatus(p.getId(),ProductStatus.PENDING_APPROVAL); em.flush();
        assertTrue(products.searchProducts(null,null,null,PageRequest.of(0,10)).isEmpty());
    }
    @Test void softDeletionPreservesOrderAndPaymentReferencesAndDeletesStoredBytes() throws Exception {
        var p=product(ProductStatus.ARCHIVED);
        var order=orders.saveAndFlush(Order.builder().buyerId(9L).code("AUDIT-ORDER")
            .status(OrderStatus.COMPLETED).subtotal(BigDecimal.TEN).deliveryFee(BigDecimal.ZERO)
            .total(BigDecimal.TEN).paymentStatus(PaymentStatus.PAID).addressId(1L).build());
        var line=items.saveAndFlush(OrderItem.builder().orderId(order.getId()).productId(p.getId())
            .sellerId(4L).productName(p.getName()).unit("kg").quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());
        var payment=new Payment(); payment.setOrderId(order.getId()); payment.setMethod(PaymentMethod.MPESA);
        payment.setReference("AUDIT-PAYMENT");payment.setAmount(BigDecimal.TEN);payment.setStatus(PaymentStatus.PAID);
        payments.saveAndFlush(payment);
        var image=service.uploadProductImage(p.getId(),png(),0,true); em.flush();
        assertEquals(1,jdbc.queryForObject("select count(*) from stored_images",Integer.class));
        service.deleteProduct(p.getId()); em.flush(); em.clear();
        assertEquals(ProductStatus.DELETED,products.findById(p.getId()).orElseThrow().getStatus());
        assertEquals(p.getId(),items.findById(line.getId()).orElseThrow().getProductId());
        assertEquals(order.getId(),payments.findById(payment.getId()).orElseThrow().getOrderId());
        assertEquals(PaymentStatus.PAID,payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(0,jdbc.queryForObject("select count(*) from stored_images",Integer.class));
        assertTrue(images.findByProductId(p.getId()).isEmpty());
        assertTrue(products.findFilteredProducts(null,null,null,null,PageRequest.of(0,10)).isEmpty());
    }
    @Test void nativeFarmFiltersAndCountsIncludeAssociatedFarmsWithoutVerificationGate() {
        var farmer = farmerAccounts.saveAndFlush(com.kilivana.backend.admin.entity.User.builder()
            .name("Audit Farmer").email("audit-farmer@example.invalid").phone("0700000000")
            .passwordHash("isolated-test-hash").role(UserRole.FARMER).status(UserStatus.ACTIVE)
            .verificationStatus(VerificationStatus.PENDING).build());
        Long ownerId = farmer.getId();
        farms.saveAndFlush(Farm.builder().farmerId(ownerId).name("Green Valley").county("Kiambu").sizeAcres(2.0)
            .ownershipType(OwnershipType.OWNED).status(FarmStatus.ACTIVE).build());
        farms.saveAndFlush(Farm.builder().farmerId(ownerId).name("Green Ridge").county("Kiambu").sizeAcres(3.0)
            .ownershipType(OwnershipType.OWNED).status(FarmStatus.FALLOW).build());
        var result=farms.searchFarms("green","Kiambu",null,ownerId,PageRequest.of(0,1));
        assertEquals(2,result.getTotalElements());assertEquals(2,result.getTotalPages());
        var active=farms.searchFarms(null,null,FarmStatus.ACTIVE,ownerId,PageRequest.of(0,10));
        assertEquals(1,active.getTotalElements());assertEquals("Green Valley",active.getContent().get(0).getName());
        assertEquals(ownerId,active.getContent().get(0).getFarmerId());
        assertTrue(farms.searchFarms(null,null,null,99L,PageRequest.of(0,10)).isEmpty());
        assertTrue(farms.searchFarms("missing",null,null,null,PageRequest.of(2,1)).isEmpty());
        assertEquals(2,farms.count());
        farmer.setVerificationStatus(VerificationStatus.VERIFIED); farmerAccounts.saveAndFlush(farmer);
        var approved=farms.searchFarms("Green Valley",null,FarmStatus.ACTIVE,ownerId,PageRequest.of(0,10));
        assertEquals(1,approved.getTotalElements()); assertEquals(ownerId,approved.getContent().get(0).getFarmerId());
        assertEquals("Green Valley",approved.getContent().get(0).getName());
        assertEquals(2,farms.count()); // Approval never manufactures or duplicates farms.
    }
    @Test void lifecycleMigrationPreservesOldStatesAndAllowsNewOnes() throws Exception {
        // Execute the exact migration DDL inside this rollback-only fixture transaction.
        String sql=Files.readString(Path.of("db/migrations/V19__seller_product_lifecycle.sql"));
        for(String statement:sql.replaceAll("(?m)--.*$", "").split(";")) {
            String ddl=statement.trim();
            if(!ddl.isEmpty() && !ddl.equals("BEGIN") && !ddl.equals("COMMIT")) jdbc.execute(ddl);
        }
        for(var state:ProductStatus.values()) assertEquals(state,product(state).getStatus());
        assertEquals(ProductStatus.values().length,products.count());
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void checkedUploadFailureRollsBackProductFieldsAndImageCollection() throws Exception {
        var tx=new TransactionTemplate(transactions);
        Long id=tx.execute(status->product(ProductStatus.UNLISTED).getId());
        try {
            ImageStorage failing=mock(ImageStorage.class);
            when(failing.uploadImage(any(),anyString())).thenThrow(new IOException("simulated upload failure"));
            var real=new ProductService(products,images,mock(CategoryRepository.class),failing,users,visibility);
            var factory=new org.springframework.aop.framework.ProxyFactory(real);
            var interceptor=new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
            factory.addAdvice(interceptor);
            var proxied=(ProductService)factory.getProxy();
            assertThrows(IOException.class,()->proxied.updateProductWithImages(id,
                ProductRequest.builder().name("Changed").description("Changed").unit("kg").price(BigDecimal.ONE)
                .stockQty(10).minimumOrderQty(1).build(),List.of(png())));
            tx.executeWithoutResult(status->{ em.clear();var reloaded=products.findById(id).orElseThrow();
                assertEquals("Audit Tomatoes",reloaded.getName());assertEquals("Original",reloaded.getDescription());
                assertEquals(0,BigDecimal.TEN.compareTo(reloaded.getPrice()));assertTrue(images.findByProductId(id).isEmpty()); });
        } finally { tx.executeWithoutResult(status->products.deleteById(id)); }
    }
}
