package com.kilivana.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves locally stored uploads over HTTP.
 *
 * <p>Only active for the local filesystem provider. Database storage returns its own URLs, so
 * nothing here should shadow them.
 */
@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final String directory;
    private final com.kilivana.backend.security.ProductVisibility visibility;

    public LocalStorageWebConfig(@Value("${app.storage.local.directory:./uploads}") String directory,
            com.kilivana.backend.security.ProductVisibility visibility) {
        this.directory = directory;
        this.visibility = visibility;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response, Object handler) {
                if (request.getRequestURI().contains("//"))
                    throw new com.kilivana.backend.common.exception.ResourceNotFoundException("Image not found");
                String path = org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(request);
                String publicId = path.substring("/uploads/".length());
                // Reject alternate spellings before the resource handler normalizes the path.
                if (publicId.contains("..") || publicId.contains("\\") || publicId.contains("%")
                        || publicId.startsWith("/") || publicId.contains("//") || publicId.contains("/./"))
                    throw new com.kilivana.backend.common.exception.ResourceNotFoundException("Image not found");
                // Windows can serve case/short-name aliases of the same file. Authorize the
                // real storage-relative name, not an alias that misses its database link.
                var root = Paths.get(directory).toAbsolutePath().normalize();
                var file = root.resolve(publicId).normalize();
                if (java.nio.file.Files.exists(file)) {
                    try {
                        var realRoot = root.toRealPath();
                        var realFile = file.toRealPath();
                        if (!realFile.startsWith(realRoot))
                            throw new com.kilivana.backend.common.exception.ResourceNotFoundException("Image not found");
                        publicId = realRoot.relativize(realFile).toString().replace('\\', '/');
                    } catch (java.io.IOException e) {
                        throw new com.kilivana.backend.common.exception.ResourceNotFoundException("Image not found");
                    }
                }
                visibility.requireImageReadable(publicId, publicId.startsWith("kilivana_products_")
                        || publicId.startsWith("products/"));
                response.setHeader("Cache-Control", "no-store");
                return true;
            }
        }).addPathPatterns("/uploads/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(directory).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCacheControl(org.springframework.http.CacheControl.noStore());
    }
}
