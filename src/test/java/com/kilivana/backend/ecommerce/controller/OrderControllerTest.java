package com.kilivana.backend.ecommerce.controller;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.ecommerce.dto.OrderStatsResponse;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    @Test
    void globalStatsEndpointReturnsApiResponseWithStats() throws Exception {
        OrderService orderService = mock(OrderService.class);
        when(orderService.getOrderManagementStats())
                .thenReturn(new OrderStatsResponse(150L, 20L, 30L, 40L, 45L, 15L));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(
                orderService, mock(UserRepository.class), mock(OrderItemRepository.class))).build();

        mockMvc.perform(get("/api/v1/orders/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(150))
                .andExpect(jsonPath("$.data.placed").value(20))
                .andExpect(jsonPath("$.data.confirmed").value(30))
                .andExpect(jsonPath("$.data.processing").value(40))
                .andExpect(jsonPath("$.data.delivered").value(45))
                .andExpect(jsonPath("$.data.cancelled").value(15));
    }
}
