package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.admin.entity.Address;
import com.kilivana.backend.admin.repository.AddressRepository;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.security.CurrentUser;
import com.kilivana.backend.ecommerce.dto.CheckoutOrderRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteRequest;
import com.kilivana.backend.ecommerce.dto.DeliveryQuoteResponse;
import com.kilivana.backend.ecommerce.entity.Cart;
import com.kilivana.backend.ecommerce.entity.CartItem;
import com.kilivana.backend.ecommerce.entity.Order;
import com.kilivana.backend.ecommerce.entity.OrderItem;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.CartItemRepository;
import com.kilivana.backend.ecommerce.repository.CartRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("150.00");
    private static final int QUOTE_TTL_MINUTES = 15;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;

    private final Map<String, QuoteRecord> quotes = new ConcurrentHashMap<>();

    public record QuoteRecord(String quoteId, Long userId, Long cartId, Long addressId,
                             BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total,
                             Instant expiresAt, boolean used) {}

    @Transactional
    public DeliveryQuoteResponse createDeliveryQuote(DeliveryQuoteRequest request) {
        Long userId = CurrentUser.id();
        if (userId == null) throw new BadRequestException("Authentication required");

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", request.getAddressId()));
        if (!address.getUserId().equals(userId)) {
            throw new BadRequestException("Address does not belong to the authenticated user");
        }
        if (address.getLatitude() == null || address.getLongitude() == null) {
            throw new BadRequestException("Delivery address is not geocoded/valid");
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
        BigDecimal total = subtotal.add(DELIVERY_FEE);
        String quoteId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(QUOTE_TTL_MINUTES, ChronoUnit.MINUTES);

        quotes.put(quoteId, new QuoteRecord(quoteId, userId, cart.getId(), address.getId(),
                subtotal, DELIVERY_FEE, total, expiresAt, false));

        return DeliveryQuoteResponse.builder()
                .quoteId(quoteId)
                .addressId(address.getId())
                .subtotal(subtotal)
                .deliveryFee(DELIVERY_FEE)
                .total(total)
                .currency("KES")
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public Long createOrderFromQuote(CheckoutOrderRequest request) {
        Long userId = CurrentUser.id();
        if (userId == null) throw new BadRequestException("Authentication required");

        QuoteRecord quote = quotes.get(request.getQuoteId());
        if (quote == null) throw new BadRequestException("Quote not found");
        if (!quote.userId().equals(userId)) throw new BadRequestException("Quote does not belong to this user");
        if (quote.used()) throw new BadRequestException("Quote already used");
        if (Instant.now().isAfter(quote.expiresAt())) throw new BadRequestException("Quote expired");

        Cart cart = cartRepository.findById(quote.cartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", quote.cartId()));
        if (cart.getStatus() != Cart.CartStatus.CHECKOUT_PROCESSING) {
            throw new BadRequestException("Cart is no longer in checkout");
        }
        java.util.List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Recalculate server-side
        BigDecimal subtotal = BigDecimal.ZERO;
        // build items list for OrderService using product prices
        java.util.List<com.kilivana.backend.ecommerce.dto.OrderItemRequest> items = new java.util.ArrayList<>();
        for (CartItem ci : cartItems) {
            Product p = productRepository.findById(ci.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", ci.getProductId()));
            items.add(com.kilivana.backend.ecommerce.dto.OrderItemRequest.builder()
                    .productId(p.getId())
                    .quantity(ci.getQuantity())
                    .unitPrice(p.getPrice())
                    .build());
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        BigDecimal total = subtotal.add(quote.deliveryFee());

        Order order = orderService.createOrder(userId, quote.addressId(), quote.deliveryFee(), items, subtotal, total);

        // mark quote used
        quotes.put(quote.quoteId(), new QuoteRecord(quote.quoteId(), quote.userId(), quote.cartId(),
                quote.addressId(), quote.subtotal(), quote.deliveryFee(), quote.total(), quote.expiresAt(), true));

        // transition cart out of checkout processing
        cart.setStatus(Cart.CartStatus.ABANDONED);
        cartRepository.save(cart);

        return order.getId();
    }
}
