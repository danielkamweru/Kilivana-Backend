package com.kilivana.backend.service;

import com.kilivana.backend.config.DarajaProperties;
import com.kilivana.backend.common.exception.ServiceUnavailableException;
import com.kilivana.backend.dto.StkPushResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DarajaService} that do not require a live Safaricom call.
 *
 * <p>The STK password is a pure function of shortcode, passkey and timestamp, so it
 * is verified directly against the documented formula. The missing-credentials path
 * is checked because it must fail loudly rather than returning a null token.
 */
class DarajaServiceTest {

    @Test
    void generateStkPasswordMatchesTheDarajaSpecification() {
        DarajaProperties properties = new DarajaProperties();
        properties.setShortcode("174379");
        properties.setPasskey("bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919");

        DarajaService service = new DarajaService(properties, mock(WebClient.class));

        String timestamp = "20210628092408";
        String raw = properties.getShortcode() + properties.getPasskey() + timestamp;
        String expected = new String(Base64.getEncoder().encode(raw.getBytes()));

        String actual = invokeGenerateStkPassword(service, timestamp);

        assertThat(actual).isEqualTo(expected);
        // Daraja rejects an empty password, so the helper must never yield one.
        assertThat(actual).isNotEmpty();
    }

    @Test
    void missingCredentialsAreRejectedBeforeAnyNetworkCall() {
        DarajaProperties properties = new DarajaProperties();
        // consumer key/secret/passkey left blank
        DarajaService service = new DarajaService(properties, mock(WebClient.class));

        assertThatThrownBy(service::getValidAccessToken)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("not configured");
    }

    private static String invokeGenerateStkPassword(DarajaService service, String timestamp) {
        try {
            java.lang.reflect.Method m = DarajaService.class.getDeclaredMethod(
                    "generateStkPassword", String.class);
            m.setAccessible(true);
            return (String) m.invoke(service, timestamp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}