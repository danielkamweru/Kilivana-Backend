package com.kilivana.backend.repository;

import com.kilivana.backend.admin.entity.AuditLog;
import com.kilivana.backend.admin.repository.AuditLogRepository;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the optional-filter repository queries.
 *
 * <p>These queries filter with a "(:param IS NULL OR column = :param)" pattern. When the
 * parameter is uncast, PostgreSQL cannot determine the type of the parameter used in the
 * null check and rejects the statement at execution time with
 * "could not determine data type of parameter". That failure only appears when the
 * endpoint is actually called with filters omitted, so it needs an explicit test.
 *
 * <p>Runs against the configured PostgreSQL instance rather than an embedded database,
 * because the bug under test is PostgreSQL-specific parameter typing.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OptionalFilterQueryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private static final Pageable PAGE = PageRequest.of(0, 10);

    @Test
    void searchAuditLogs_shouldAcceptEveryFilterOmitted() {
        assertThat(auditLogRepository.searchAuditLogs(null, null, null, null, null, PAGE)).isNotNull();
    }

    @Test
    void searchAuditLogs_shouldAcceptEachFilterIndividually() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        assertThat(auditLogRepository.searchAuditLogs(1L, null, null, null, null, PAGE)).isNotNull();
        assertThat(auditLogRepository.searchAuditLogs(null, "PRODUCT", null, null, null, PAGE)).isNotNull();
        assertThat(auditLogRepository.searchAuditLogs(null, null, "CREATE", null, null, PAGE)).isNotNull();
        assertThat(auditLogRepository.searchAuditLogs(null, null, null, from, null, PAGE)).isNotNull();
        assertThat(auditLogRepository.searchAuditLogs(null, null, null, null, to, PAGE)).isNotNull();
    }

    @Test
    void searchAuditLogs_shouldAcceptAllFiltersTogether() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        assertThat(auditLogRepository.searchAuditLogs(1L, "PRODUCT", "CREATE", from, to, PAGE)).isNotNull();
    }

    @Test
    void searchOrders_shouldAcceptFiltersOmitted() {
        assertThat(orderRepository.searchOrders(null, null, PAGE)).isNotNull();
        assertThat(orderRepository.searchOrders(1L, null, PAGE)).isNotNull();
        assertThat(orderRepository.searchOrders(null, OrderStatus.PLACED, PAGE)).isNotNull();
        assertThat(orderRepository.searchOrders(1L, OrderStatus.PLACED, PAGE)).isNotNull();
    }

    @Test
    void searchProducts_shouldAcceptFiltersOmitted() {
        assertThat(productRepository.searchProducts(null, null, null, PAGE)).isNotNull();
        assertThat(productRepository.searchProducts("apple", null, null, PAGE)).isNotNull();
        assertThat(productRepository.searchProducts(null, 1L, null, PAGE)).isNotNull();
        assertThat(productRepository.searchProducts(null, null, "FARMER", PAGE)).isNotNull();
        assertThat(productRepository.searchProducts("apple", 1L, "FARMER", PAGE)).isNotNull();
    }

    @Test
    void orderShouldPersistWithTimestampsPopulated() {
        Order order = Order.builder()
                .buyerId(1L)
                // The placement service assigns the reference; a row written
                // outside it has to carry one, as the column is not null.
                .code("ORD-TEST-001")
                .status(OrderStatus.PLACED)
                .subtotal(new BigDecimal("100.00"))
                .deliveryFee(new BigDecimal("10.00"))
                .total(new BigDecimal("110.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .addressId(1L)
                .build();

        Order saved = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void auditLogShouldPersistWithTimestampsPopulated() {
        AuditLog log = AuditLog.builder()
                .actorId(1L)
                .entityType("PRODUCT")
                .entityId(1L)
                .action("CREATE")
                .build();

        AuditLog saved = auditLogRepository.save(log);
        entityManager.flush();
        entityManager.clear();

        AuditLog reloaded = auditLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
    }
}
