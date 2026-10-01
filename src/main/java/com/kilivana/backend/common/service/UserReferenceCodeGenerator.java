package com.kilivana.backend.common.service;

import com.kilivana.backend.admin.repository.UserRepository;
import com.kilivana.backend.common.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Hands out the per-role reference codes such as {@code F-014}.
 *
 * <p>Shared by self-registration and administrator-created accounts so both paths assign a code;
 * administrator-created users used to be saved with a null code.
 */
@Component
@RequiredArgsConstructor
public class UserReferenceCodeGenerator {

    private final UserRepository userRepository;

    /**
     * The next code for a role. Derived from the highest code already handed out rather than
     * the row count, because deleting an account made the count smaller than the sequence and
     * the next registration collided with an existing unique code. Two registrations racing can
     * still collide, so a busy deployment should move this to a database sequence.
     */
    public String nextCode(UserRole role) {
        long highest = userRepository.findReferenceCodesByRole(role).stream()
                .mapToLong(UserReferenceCodeGenerator::sequenceOf)
                .max()
                .orElse(0L);
        return role.referenceCode(highest + 1);
    }

    /** The numeric part of {@code F-014}, or 0 for a code in an unexpected shape. */
    private static long sequenceOf(String referenceCode) {
        int dash = referenceCode == null ? -1 : referenceCode.lastIndexOf('-');
        if (dash < 0) {
            return 0L;
        }
        try {
            return Long.parseLong(referenceCode.substring(dash + 1));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}