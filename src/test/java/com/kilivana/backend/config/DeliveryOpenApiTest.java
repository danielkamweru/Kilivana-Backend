package com.kilivana.backend.config;

import com.kilivana.backend.admin.controller.AdminOperationsController;
import com.kilivana.backend.admin.controller.SupplierWarehouseController;
import com.kilivana.backend.admin.service.DashboardService;
import com.kilivana.backend.admin.service.SupplierWarehouseService;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.ecommerce.repository.PaymentRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.ecommerce.service.ProductService;
import com.kilivana.backend.logistics.controller.AdminPickupStationController;
import com.kilivana.backend.logistics.controller.BuyerDeliveryController;
import com.kilivana.backend.logistics.controller.LogisticsJobController;
import com.kilivana.backend.logistics.controller.PickupStationController;
import com.kilivana.backend.logistics.controller.SellerDeliveryController;
import com.kilivana.backend.logistics.controller.TripsController;
import com.kilivana.backend.logistics.service.DeliveryDispatchService;
import com.kilivana.backend.logistics.service.DriverLocationService;
import com.kilivana.backend.logistics.service.LogisticsService;
import com.kilivana.backend.logistics.service.PickupStationService;
import com.kilivana.backend.logistics.service.TrackingAuthorizationService;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminOperationsController.class,
        AdminPickupStationController.class,
        SupplierWarehouseController.class,
        PickupStationController.class,
        LogisticsJobController.class,
        TripsController.class,
        BuyerDeliveryController.class,
        SellerDeliveryController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(OpenApiConfig.class)
@ImportAutoConfiguration({
        SpringDocConfigProperties.class,
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class
})
class DeliveryOpenApiTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;
    @MockBean private OrderService orderService;
    @MockBean private PaymentService paymentService;
    @MockBean private LogisticsService logisticsService;
    @MockBean private DeliveryDispatchService deliveryDispatchService;
    @MockBean private DashboardService dashboardService;
    @MockBean private OrderRepository orderRepository;
    @MockBean private PaymentRepository paymentRepository;
    @MockBean private LogisticsJobRepository logisticsJobRepository;
    @MockBean private PickupStationService pickupStationService;
    @MockBean private SupplierWarehouseService supplierWarehouseService;
    @MockBean private DriverLocationService driverLocationService;
    @MockBean private TrackingAuthorizationService trackingAuthorizationService;

    @Test
    void runtimeOpenApiDocumentsTheAddedDeliveryAndPickupRoutes() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/admin/pickup-stations'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/pickup-stations/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/pickup-stations/nearest'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/suppliers/me/warehouses'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/suppliers/me/warehouses/{id}/default'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/orders/{orderId}/dispatch'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/logistics/jobs/{id}/reject'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/trips/jobs/{jobId}/accept'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/trips/{tripId}/location'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/trips/{tripId}/proof'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/buyer/orders/{orderId}/delivery'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/seller/orders/{orderId}/deliveries'].get").exists());
    }
}
