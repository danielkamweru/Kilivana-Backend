package com.kilivana.backend.logistics.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static helpers that have no Spring dependency, so they are tested directly. Pinning the
 * wire format here means a refactor of the destination strings cannot silently break the
 * client/server contract: the Android app subscribes to these exact paths.
 */
class TrackingDestinationTest {

    @Test
    void userDestinationUsesSpringPerUserQueue() {
        assertThat(TrackingBroadcastService.userDestination(7L)).isEqualTo("/user/7/tracking");
    }

    @Test
    void tripDestinationIsTopicScopedByJob() {
        assertThat(TrackingBroadcastService.tripDestination(42L)).isEqualTo("/topic/tracking/42");
    }
}