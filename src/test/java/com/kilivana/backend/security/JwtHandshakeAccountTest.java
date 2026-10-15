package com.kilivana.backend.security;

import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.HashMap;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtHandshakeAccountTest {
    @Test void handshakeRechecksCurrentAccountBeforePublishingSessionIdentity() {
        JwtService jwt = mock(JwtService.class);
        AccountAccessValidator accounts = mock(AccountAccessValidator.class);
        var interceptor = new JwtHandshakeInterceptor(jwt, accounts);
        var servlet = new MockHttpServletRequest("GET", "/ws");
        servlet.setQueryString("token=fixture");
        var request = new ServletServerHttpRequest(servlet);
        when(jwt.isTokenType("fixture", JwtService.TYPE_ACCESS)).thenReturn(true);
        when(jwt.extractUserId("fixture")).thenReturn(7L);
        when(jwt.extractRole("fixture")).thenReturn(UserRole.FARMER);
        var attributes = new HashMap<String, Object>();
        assertTrue(interceptor.beforeHandshake(request, null, null, attributes));
        verify(accounts).validate(7L, UserRole.FARMER);
        assertEquals(7L, attributes.get(JwtHandshakeInterceptor.USER_ID_PROPERTY));
        attributes.clear();
        doThrow(new UnauthorizedException("Invalid access token")).when(accounts).validate(7L, UserRole.FARMER);
        assertFalse(interceptor.beforeHandshake(request, null, null, attributes));
        assertTrue(attributes.isEmpty());
    }
}
