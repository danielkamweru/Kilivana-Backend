package com.kilivana.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kilivana.backend.config.DarajaProperties;
import com.kilivana.backend.dto.StkPushResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the STK Push request the application actually puts on the wire.
 *
 * <p>The Daraja sandbox is not a reliable test target: it rate-limits by client
 * address, answers with a web challenge page instead of JSON, and refuses requests
 * whose shortcode and passkey do not belong to the same Daraja application as the
 * consumer key. A failure there therefore says nothing about the request this code
 * builds. This test points the client at a local stub and asserts the request
 * against the Daraja specification, so a broken request is caught here instead of
 * being blamed on the sandbox.
 */
class DarajaStkRequestTest {

    private HttpServer stub;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void startStub() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/oauth/v1/generate", exchange -> {
            respond(exchange, "{\"access_token\":\"tok-abc\",\"expires_in\":\"3599\",\"token_type\":\"Bearer\"}");
        });
        stub.createContext("/mpesa/stkpush/v1/processrequest", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"MerchantRequestID\":\"mr-TEST\",\"CheckoutRequestID\":\"ws_CO_TEST\","
                    + "\"ResponseCode\":\"0\",\"ResponseDescription\":\"Success\"}");
        });
        stub.start();
    }

    @AfterEach
    void stopStub() {
        if (stub != null) {
            stub.stop(0);
        }
    }

    @Test
    void theStkPushRequestMatchesTheDarajaSpecification() throws Exception {
        DarajaProperties properties = new DarajaProperties();
        properties.setConsumerKey("consumer-key");
        properties.setConsumerSecret("consumer-secret");
        properties.setShortcode("174379");
        properties.setPasskey("passkey-123");
        properties.setCallbackUrl("https://example.test/api/v1/payments/daraja/callback");

        DarajaService service = new DarajaService(properties, stubClient());

        StkPushResponse response = service.initiateStkPush("254708374149", 485, "ORD-SEED-003", "Payment");

        assertThat(response.getCheckoutRequestID()).isEqualTo("ws_CO_TEST");
        assertThat(response.getResponseCode()).isEqualTo("0");
        assertThat(authorization.get()).isEqualTo("Bearer tok-abc");

        JsonNode body = new ObjectMapper().readTree(requestBody.get());
        assertThat(body.get("BusinessShortCode").asText()).isEqualTo("174379");
        assertThat(body.get("TransactionType").asText()).isEqualTo("CustomerPayBillOnline");
        assertThat(body.get("Amount").isInt()).isTrue();
        assertThat(body.get("Amount").asInt()).isEqualTo(485);
        assertThat(body.get("PartyA").asText()).isEqualTo("254708374149");
        assertThat(body.get("PartyB").asText()).isEqualTo("174379");
        assertThat(body.get("PhoneNumber").asText()).isEqualTo("254708374149");
        assertThat(body.get("CallBackURL").asText()).isEqualTo("https://example.test/api/v1/payments/daraja/callback");
        assertThat(body.get("AccountReference").asText()).isEqualTo("ORD-SEED-003");
        assertThat(body.get("TransactionDesc").asText()).isEqualTo("Payment");
    }

    @Test
    void theStkPasswordIsTheBase64OfTheShortcodePasskeyAndTimestamp() throws Exception {
        DarajaProperties properties = new DarajaProperties();
        properties.setConsumerKey("consumer-key");
        properties.setConsumerSecret("consumer-secret");
        properties.setShortcode("174379");
        properties.setPasskey("passkey-123");
        properties.setCallbackUrl("https://example.test/api/v1/payments/daraja/callback");

        DarajaService service = new DarajaService(properties, stubClient());
        service.initiateStkPush("254708374149", 485, "ORD-SEED-003", "Payment");

        String password = new String(Base64.getDecoder().decode(
                new ObjectMapper().readTree(requestBody.get()).get("Password").asText()), StandardCharsets.UTF_8);
        assertThat(password).startsWith("174379passkey-123");

        String timestamp = password.substring("174379passkey-123".length());
        assertThat(timestamp).hasSize(14);
        LocalDateTime parsed = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        assertThat(parsed).isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(1));
    }

    /** A client whose sandbox calls are redirected to the local stub. */
    private WebClient stubClient() {
        String base = "http://127.0.0.1:" + stub.getAddress().getPort();
        return WebClient.builder()
                .filter((request, next) -> next.exchange(ClientRequest.from(request)
                        .url(URI.create(request.url().toString().replace("https://sandbox.safaricom.co.ke", base)))
                        .build()))
                .build();
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
