package com.kilivana.backend.admin.service;

import com.kilivana.backend.admin.dto.AdminBuyerResponse;
import com.kilivana.backend.admin.entity.BuyerProfile;
import com.kilivana.backend.admin.entity.User;
import com.kilivana.backend.admin.repository.BuyerProfileRepository;
import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.enums.UserStatus;
import com.kilivana.backend.ecommerce.repository.DisputeRepository;
import com.kilivana.backend.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The administrator's buyer list.
 *
 * <p>Separate from {@link ProfileService} because this is an administration concern: it reads
 * across users, profiles, orders and disputes, and none of the other roles need that.
 */
@Service
@RequiredArgsConstructor
public class AdminBuyerService {

    private final UserRepository userRepository;
    private final BuyerProfileRepository buyerProfileRepository;
    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;

    /**
     * Lists every buyer account with its profile, order count, total spend and dispute count.
     *
     * <p>A buyer without a profile is still listed, with profile fields null, because a registered
     * buyer who has not onboarded a profile is still a buyer the panel has to show.
     */
    @Transactional(readOnly = true)
    public List<AdminBuyerResponse> listBuyers() {
        List<User> buyers = userRepository.findByRole(UserRole.BUYER);

        Map<Long, BuyerProfile> profiles = new HashMap<>();
        for (BuyerProfile profile : buyerProfileRepository.findAll()) {
            profiles.put(profile.getUserId(), profile);
        }

        Map<Long, Long> orderCounts = new HashMap<>();
        Map<Long, BigDecimal> totalSpends = new HashMap<>();
        for (Object[] row : orderRepository.countAndSumTotalByBuyerId()) {
            Long buyerId = (Long) row[0];
            orderCounts.put(buyerId, (Long) row[1]);
            totalSpends.put(buyerId, (BigDecimal) row[2]);
        }

        Map<Long, Long> disputeCounts = new HashMap<>();
        for (Object[] row : disputeRepository.countByBuyerId()) {
            disputeCounts.put((Long) row[0], (Long) row[1]);
        }

        return buyers.stream()
                .map(user -> toResponse(user, profiles.get(user.getId()),
                        orderCounts.getOrDefault(user.getId(), 0L),
                        totalSpends.getOrDefault(user.getId(), BigDecimal.ZERO),
                        disputeCounts.getOrDefault(user.getId(), 0L)))
                .toList();
    }

    private AdminBuyerResponse toResponse(User user, BuyerProfile profile,
                                          long ordersCount, BigDecimal totalSpend, long disputesCount) {
        return AdminBuyerResponse.builder()
                .userId(user.getId())
                .profileId(profile == null ? null : profile.getId())
                .code(user.getReferenceCode())
                .fullName(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .region(user.getRegion())
                .address(profile == null ? null : profile.getSavedAddresses())
                .status(mapStatus(user.getStatus()))
                .type(null)
                .ordersCount(ordersCount)
                .totalSpend(totalSpend)
                .disputesCount(disputesCount)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Maps the backend's user status to the panel's buyer status vocabulary:
     * VERIFIED, PENDING, or SUSPENDED.
     */
    private static String mapStatus(UserStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "verified";
            case PENDING_VERIFICATION -> "pending";
            case SUSPENDED -> "suspended";
            case INACTIVE -> "suspended";
        };
    }
}
