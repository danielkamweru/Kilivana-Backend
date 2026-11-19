package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.dto.StkPushInitiateResponse;
import com.kilivana.backend.security.CurrentUser;
import com.kilivana.backend.ecommerce.dto.CheckoutOrderRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteResponse;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.entity.CheckoutQuote;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.repository.CheckoutQuoteRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import com.kilivana.backend.ecommerce.service.PaymentService;
import com.kilivana.backend.logistics.dto.PickupStationResponse;
import com.kilivana.backend.logistics.entity.PickupStation;
import com.kilivana.backend.logistics.repository.PickupStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final BigDecimal DOOR_DELIVERY_FEE = new BigDecimal("1.00");
    private static final BigDecimal PICKUP_STATION_FEE = new BigDecimal("1.00");
    private static final int QUOTE_TTL_MINUTES = 15;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PickupStationRepository pickupStationRepository;
    private final CheckoutQuoteRepository checkoutQuoteRepository;

    @Transactional
    public DeliveryQuoteResponse createDeliveryQuote(DeliveryQuoteRequest request) {
        Long userId = CurrentUser.id();
        if (userId == null) throw new BadRequestException("Authentication required");

        String deliveryMethod = request.getDeliveryMethod();
        if (deliveryMethod == null) {
            throw new BadRequestException("Delivery method is required");
        }

        Long addressId = null;
        Long pickupStationId = null;
        PickupStation selectedStation = null;
        String pickupLocation = null;
        BigDecimal deliveryFee;

        if ("DOOR_DELIVERY".equalsIgnoreCase(deliveryMethod)) {
            if (request.getAddressId() == null) {
                throw new BadRequestException("Address ID is required for door delivery");
            }
            Address address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));
            if (!address.getUserId().equals(userId)) {
                throw new BadRequestException("Address does not belong to the authenticated user");
            }
            addressId = address.getId();
            deliveryFee = DOOR_DELIVERY_FEE;
        } else if ("PICKUP_STATION".equalsIgnoreCase(deliveryMethod)) {
            if (request.getPickupStationId() != null) {
                if (request.getPickupLocation() != null && !request.getPickupLocation().isBlank()) {
                    throw new BadRequestException("Provide a pickup station ID or legacy pickup location, not both");
                }
                selectedStation = pickupStationRepository.findById(request.getPickupStationId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "PickupStation", request.getPickupStationId()));
                if (!Boolean.TRUE.equals(selectedStation.getIsAvailable())) {
                    throw new BadRequestException("Pickup station is not currently available");
                }
                pickupStationId = selectedStation.getId();
            } else {
                if (request.getPickupLocation() == null || request.getPickupLocation().isBlank()) {
                    throw new BadRequestException("Pickup location is required for pickup delivery");
                }
                pickupLocation = request.getPickupLocation().trim();
                if (pickupLocation.length() > 500) {
                    throw new BadRequestException("Pickup location must be at most 500 characters");
            }
            }
            deliveryFee = PICKUP_STATION_FEE;
        } else {
            throw new BadRequestException("Invalid delivery method. Must be DOOR_DELIVERY or PICKUP_STATION");
        }

        Cart cart = cartRepository.findFirstByBuyerIdAndStatus(userId, Cart.CartStatus.CHECKOUT_PROCESSING)
                .orElseThrow(() -> new BadRequestException("No checkout in progress; call POST /api/v1/checkout first"));
        java.util.List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProductId()));
            BigDecimal line = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(line);
        }
        BigDecimal total = subtotal.add(deliveryFee);
        String quoteId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(QUOTE_TTL_MINUTES, ChronoUnit.MINUTES);

        checkoutQuoteRepository.save(CheckoutQuote.builder()
                .quoteId(quoteId)
                .userId(userId)
                .cartId(cart.getId())
                .deliveryMethod(deliveryMethod)
                .addressId(addressId)
                .pickupStationId(pickupStationId)
                .pickupLocation(pickupLocation)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .expiresAt(expiresAt)
                .used(false)
                .build());

        DeliveryQuoteResponse.DeliveryQuoteResponseBuilder responseBuilder = DeliveryQuoteResponse.builder()
                .quoteId(quoteId)
                .deliveryMethod(deliveryMethod)
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .total(total)
                .currency("KES")
                .expiresAt(expiresAt);

        if (addressId != null) {
            responseBuilder.addressId(addressId);
        }
        if (selectedStation != null) {
            responseBuilder.pickupStationId(selectedStation.getId())
                    .pickupStation(PickupStationResponse.fromEntity(selectedStation));
        }
        if (pickupLocation != null) {
            responseBuilder.pickupLocation(pickupLocation);
        }

        return responseBuilder.build();
    }

    @Transactional
    public StkPushInitiateResponse createOrderFromQuote(CheckoutOrderRequest request) {
        Long userId = CurrentUser.id();
        if (userId == null) throw new BadRequestException("Authentication required");

        CheckoutQuote quote = checkoutQuoteRepository.findByQuoteIdForUpdate(request.getQuoteId())
                .orElseThrow(() -> new BadRequestException("Quote not found"));
        if (!quote.getUserId().equals(userId)) {
            throw new BadRequestException("Quote does not belong to this user");
        }
        if (quote.isUsed()) {
            if (quote.getOrderId() == null) throw new BadRequestException("Quote already used");
            // Order already created from a previous successful payment; return it.
            Order order = orderService.getOrderById(quote.getOrderId());
            StkPushInitiateResponse response = new StkPushInitiateResponse();
            response.setPaymentId(null);
            response.setOrderId(order.getId());
            response.setAmount(order.getTotal());
            response.setStatus("PAID");
            return response;
        }
        return createOrderFromQuoteLocked(userId, quote, request.getPhoneNumber());
    }

    private StkPushInitiateResponse createOrderFromQuoteLocked(Long userId, CheckoutQuote quote,
                                                               String phoneNumber) {
        if (Instant.now().isAfter(quote.getExpiresAt())) throw new BadRequestException("Quote expired");

        Cart cart = cartRepository.findByIdForUpdate(quote.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", quote.getCartId()));
        if (cart.getStatus() != Cart.CartStatus.CHECKOUT_PROCESSING) {
            throw new BadRequestException("Cart is no longer in checkout");
        }
        java.util.List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Payment-first: initiate the STK Push against the quote. No order row is
        // created yet; the order is only created once the Daraja callback reports
        // success. If payment is cancelled or fails, no order exists.
        return paymentService.initiateStkPushForQuote(userId, quote.getQuoteId(), phoneNumber);
    }
}
