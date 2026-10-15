package com.kilivana.backend.security;

import com.kilivana.backend.config.LocalStorageWebConfig;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalProductImageVisibilityTest {
    final ProductVisibility visibility = mock(ProductVisibility.class);
    HandlerInterceptor interceptor() {
        return interceptor("./uploads");
    }
    HandlerInterceptor interceptor(String directory) {
        var registry = new InterceptorRegistry();
        new LocalStorageWebConfig(directory, visibility).addInterceptors(registry);
        var interceptors = (java.util.List<?>) ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
        return (HandlerInterceptor) interceptors.get(0);
    }
    @Test void resourceHandlerCannotBypassPendingVisibility() throws Exception {
        doThrow(new ResourceNotFoundException("Image not found"))
                .when(visibility).requireImageReadable("kilivana_products_10/photo.png", true);
        var request = new MockHttpServletRequest("GET", "/uploads/kilivana_products_10/photo.png");
        assertThrows(ResourceNotFoundException.class,
                () -> interceptor().preHandle(request, new MockHttpServletResponse(), new Object()));
    }
    @Test void authorizedLocalReadUsesLinkCheckAndNoStore() throws Exception {
        var response = new MockHttpServletResponse();
        assertTrue(interceptor().preHandle(new MockHttpServletRequest("GET", "/uploads/kilivana_products_10/photo.png"),
                response, new Object()));
        verify(visibility).requireImageReadable("kilivana_products_10/photo.png", true);
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
    @Test void existingFileAliasesAuthorizeTheRealStorageName(@org.junit.jupiter.api.io.TempDir java.nio.file.Path root) throws Exception {
        var file = root.resolve("kilivana_products_10/photo.png");
        java.nio.file.Files.createDirectories(file.getParent());
        java.nio.file.Files.write(file, new byte[]{1});
        String alias = "KILIVANA_PRODUCTS_10/PHOTO.PNG";
        if (!java.nio.file.Files.exists(root.resolve(alias))) alias = "kilivana_products_10/photo.png";
        assertTrue(interceptor(root.toString()).preHandle(new MockHttpServletRequest("GET", "/uploads/" + alias),
                new MockHttpServletResponse(), new Object()));
        verify(visibility).requireImageReadable("kilivana_products_10/photo.png", true);
    }
    @ParameterizedTest @ValueSource(strings={"/uploads//kilivana_products_10/photo.png",
            "/uploads/kilivana_products_10/../photo.png", "/uploads/kilivana_products_10/./photo.png",
            "/uploads/kilivana_products_10//photo.png", "/uploads/%252e%252e/photo.png"})
    void alternatePathSpellingsCannotSkipProductLinkLookup(String path) {
        assertThrows(ResourceNotFoundException.class,
                () -> interceptor().preHandle(new MockHttpServletRequest("GET", path),
                        new MockHttpServletResponse(), new Object()));
        verifyNoInteractions(visibility);
    }
}
