package com.kilivana.backend.logistics.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.admin.repository.SupplierWarehouseRepository;
import com.kilivana.backend.common.enums.FarmStatus;
import com.kilivana.backend.common.enums.OrderStatus;
import com.kilivana.backend.common.enums.PaymentStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.repository.OrderItemRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import com.kilivana.backend.farm.entity.Farm;
import com.kilivana.backend.farm.repository.FarmRepository;
import com.kilivana.backend.logistics.entity.LogisticsJob;
import com.kilivana.backend.logistics.repository.LogisticsJobRepository;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryDispatchServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private LogisticsJobRepository jobRepository;
    @Mock private LogisticsService logisticsService;
    @Mock private FarmRepository farmRepository;
    @Mock private SupplierWarehouseRepository warehouseRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private PickupStationRepository stationRepository;
    @InjectMocks private DeliveryDispatchService dispatchService;

    @Test
    void dispatchCreatesSellerJobFromVerifiedOriginAndBuyerAddress() {
        Order order = Order.builder().id(1L).buyerId(7L).addressId(4L)
                .deliveryMethod("DOOR_DELIVERY").status(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PAID).build();
        OrderItem item = OrderItem.builder().orderId(1L).sellerId(9L)
                .productName("Avocado").quantity(3)
                .dispatchLocationIdSnapshot(5L).dispatchLocationTypeSnapshot("FARM")
                .dispatchAddressSnapshot("Farm road").dispatchLatitudeSnapshot(-1.2)
                .dispatchLongitudeSnapshot(36.8).build();
        Farm farm = Farm.builder().id(5L).farmerId(9L).status(FarmStatus.ACTIVE).build();
        Address address = Address.builder().id(4L).userId(7L).addressText("Buyer road")
                .latitude(-1.3).longitude(36.9).build();
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
        when(farmRepository.findById(5L)).thenReturn(Optional.of(farm));
        when(addressRepository.findById(4L)).thenReturn(Optional.of(address));
        when(jobRepository.findByOrderIdAndSellerId(1L, 9L)).thenReturn(List.of());
        when(logisticsService.createJob(any(LogisticsJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = dispatchService.dispatchOrder(1L);

        assertThat(response.getDeliveryJobs()).hasSize(1);
        ArgumentCaptor<LogisticsJob> jobCaptor = ArgumentCaptor.forClass(LogisticsJob.class);
        verify(logisticsService).createJob(jobCaptor.capture());
        LogisticsJob created = jobCaptor.getValue();
        assertThat(created.getSellerId()).isEqualTo(9L);
        assertThat(created.getPickupFarmId()).isEqualTo(5L);
        assertThat(created.getPickupLatitude()).isEqualTo(-1.2);
        assertThat(created.getDestinationType()).isEqualTo("HOME");
        assertThat(created.getDestinationAddress()).isEqualTo("Buyer road");
    }

    @Test
    void dispatchUsesCoordinatesAddedToFarmAfterOrderCreation() {
        Order order = Order.builder().id(2L).buyerId(7L).addressId(4L)
                .deliveryMethod("DOOR_DELIVERY").status(OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PAID).build();
        OrderItem item = OrderItem.builder().orderId(2L).sellerId(9L)
                .productName("Avocado").quantity(3)
                .dispatchLocationIdSnapshot(5L).dispatchLocationTypeSnapshot("FARM")
                .dispatchAddressSnapshot("Farm road").build();
        Farm farm = Farm.builder().id(5L).farmerId(9L).status(FarmStatus.ACTIVE)
                .address("Farm road, Kiambu").latitude(-1.2).longitude(36.8).build();
        Address address = Address.builder().id(4L).userId(7L).addressText("Buyer road")
                .latitude(-1.3).longitude(36.9).build();
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(2L)).thenReturn(List.of(item));
        when(farmRepository.findById(5L)).thenReturn(Optional.of(farm));
        when(addressRepository.findById(4L)).thenReturn(Optional.of(address));
        when(jobRepository.findByOrderIdAndSellerId(2L, 9L)).thenReturn(List.of());
        when(logisticsService.createJob(any(LogisticsJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatchService.dispatchOrder(2L);

        ArgumentCaptor<LogisticsJob> jobCaptor = ArgumentCaptor.forClass(LogisticsJob.class);
        verify(logisticsService).createJob(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getPickupAddress()).isEqualTo("Farm road, Kiambu");
        assertThat(jobCaptor.getValue().getPickupLatitude()).isEqualTo(-1.2);
        assertThat(jobCaptor.getValue().getPickupLongitude()).isEqualTo(36.8);
    }

    @Test
    void dispatchRejectsUnpaidOrdersBeforeCreatingJobs() {
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(Order.builder()
                .id(1L).status(OrderStatus.PLACED).paymentStatus(PaymentStatus.PENDING).build()));

        assertThatThrownBy(() -> dispatchService.dispatchOrder(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("paid");
        verify(logisticsService, never()).createJob(any());
    }
}
