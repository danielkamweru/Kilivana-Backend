package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.ecommerce.dto.ProductImageResponse;
import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.dto.ProductResponse;
import com.kilivana.backend.ecommerce.entity.Category;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.entity.ProductImage;
import com.kilivana.backend.ecommerce.repository.CategoryRepository;
import com.kilivana.backend.ecommerce.repository.ProductImageRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.common.service.ImageStorage;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.enums.UserRole;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.admin.service.UserService;
import com.kilivana.backend.admin.dto.UserResponse;
import com.kilivana.backend.common.enums.SellerType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
    @RequiredArgsConstructor
    public class ProductService {

    /**
     * Product catalogue for farmers and suppliers.
     *
     * <p>Products belong to a seller (farmer or supplier) and a category. A newly created product
     * starts in {@code PENDING_APPROVAL} and must be activated by an administrator before it
     * appears in the buyer's catalogue. Images are stored via the configured {@link ImageStorage}
     * provider (database or local filesystem) and the first uploaded image becomes the
     * primary thumbnail.
     */

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorage imageStorage;
    private final UserService userService;
    private final com.kilivana.backend.security.ProductVisibility visibility;

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ProductResponse createProduct(ProductRequest request, Long userId) {
        UserResponse user = userService.getUserById(userId);
        SellerType sellerType = user.getRole() == UserRole.SUPPLIER ? SellerType.SUPPLIER : SellerType.FARMER;
        Product product = Product.builder()
                .sellerId(userId)
                .sellerType(sellerType)
                .categoryId(resolveCategoryId(request, sellerType))
                .name(request.getName())
                .description(request.getDescription())
                .unit(request.getUnit())
                .price(request.getPrice())
                .stockQty(request.getStockQty())
                .reservedQty(0)
                .soldQty(0)
                .minimumOrderQty(request.getMinimumOrderQty())
                .status(ProductStatus.PENDING_APPROVAL)
                .build();
        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @PreAuthorize("@resourceAuthorization.admin()")
    public ProductResponse createProduct(ProductRequest request) {
        return createProduct(request, request.getSellerId());
    }

    private Long resolveCategoryId(ProductRequest request, SellerType sellerType) {
        if (request.getCategoryId() != null) {
            return request.getCategoryId();
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            String name = request.getCategory().trim();
            return categoryRepository.findByNameIgnoreCase(name)
                    .orElseThrow(() -> new BadRequestException("Select an existing category; only administrators can create categories"))
                    .getId();
        }
        throw new BadRequestException("Category ID or category name is required");
    }

    @Transactional(readOnly = true)
    public void ensureProductOwnership(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!visibility.canReviewSeller(product.getSellerId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the seller or administrator may modify this product");
        }
        requireNotDeleted(product);
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@resourceAuthorization.seller(#userId)")
    public ProductResponse createProductWithImages(ProductRequest request, List<MultipartFile> images, Long userId) throws IOException {
        ProductResponse productResponse = createProduct(request, userId);
        
        if (images != null && !images.isEmpty()) {
            uploadImages(productResponse.getId(), images);
            productResponse = getProductById(productResponse.getId());
        }
        
        return productResponse;
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        visibility.requireReadable(product);
        return mapToResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .filter(p -> p.getStockQty() > 0)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        boolean review = visibility.canReviewSeller(sellerId);
        return productRepository.findBySellerId(sellerId).stream()
                .filter(p -> p.getStatus() != ProductStatus.DELETED)
                .filter(p -> review || p.getStatus() == ProductStatus.ACTIVE)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE && p.getStockQty() > 0)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<ProductResponse> searchProducts(String name, Long categoryId, String sellerType, Pageable pageable) {
        return productRepository.searchProducts(name, categoryId, sellerType, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#id)")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        requireNotDeleted(product);
        if (request.getCategoryId() != null
                || (request.getCategory() != null && !request.getCategory().isBlank())) {
            product.setCategoryId(resolveCategoryId(request, product.getSellerType()));
        }
        if (request.getStockQty() == null || request.getStockQty() < Math.max(0, product.getReservedQty() == null ? 0 : product.getReservedQty()))
            throw new BadRequestException("Stock cannot be less than reserved quantity");
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setUnit(request.getUnit());
        product.setPrice(request.getPrice());
        product.setStockQty(request.getStockQty());
        product.setMinimumOrderQty(request.getMinimumOrderQty());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#id)")
    public ProductResponse updateProductWithImages(Long id, ProductRequest request, List<MultipartFile> images) throws IOException {
        ProductResponse productResponse = updateProduct(id, request);
        
        if (images != null && !images.isEmpty()) {
            uploadImages(id, images);
            productResponse = getProductById(id);
        }
        
        return productResponse;
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#productId)")
    public ProductImageResponse uploadProductImage(Long productId, MultipartFile image, Integer sortOrder, Boolean isPrimary) throws IOException {
        Product product = lockEditable(productId);
        validateImage(image);
        if (productImageRepository.findByProductId(productId).size() >= MAX_PRODUCT_IMAGES)
            throw new BadRequestException("At most " + MAX_PRODUCT_IMAGES + " product images are allowed");

        if (Boolean.TRUE.equals(isPrimary)) {
            // Demote the current primary so only one image wears the badge.
            productImageRepository.findPrimaryByProductId(productId)
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    productImageRepository.save(existingPrimary);
                });
        }

        String folder = "kilivana/products/" + productId;
        Map<String, Object> uploadResult = uploadTracked(image, folder);
        
        String secureUrl = imageStorage.getSecureUrl(uploadResult);
        String publicId = imageStorage.getPublicId(uploadResult);
        String assetId = imageStorage.getAssetId(uploadResult);

        ProductImage productImage = ProductImage.builder()
                .productId(productId)
                .url(secureUrl)
                .publicId(publicId)
                .assetId(assetId)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .build();

        ProductImage saved = productImageRepository.save(productImage);
        return mapToImageResponse(saved);
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#productId)")
    public void deleteProductImage(Long productId, Long imageId) throws IOException {
        lockEditable(productId);
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        
        if (!productImage.getProductId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }
        
        deleteStoredImage(productImage.getPublicId());
        productImageRepository.delete(productImage);
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#productId)")
    public ProductImageResponse updateProductImage(Long productId, Long imageId, MultipartFile newImage, Integer sortOrder, Boolean isPrimary) throws IOException {
        lockEditable(productId);
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        
        if (!productImage.getProductId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }

        if (Boolean.TRUE.equals(isPrimary)) {
            productImageRepository.findPrimaryByProductId(productId)
                .filter(existing -> !existing.getId().equals(imageId))
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    productImageRepository.save(existingPrimary);
                });
        }

        String folder = "kilivana/products/" + productId;
        validateImage(newImage);
        String oldPublicId = productImage.getPublicId();
        Map<String, Object> uploadResult = uploadTracked(newImage, folder);
        deleteStoredImage(oldPublicId);
        
        String secureUrl = imageStorage.getSecureUrl(uploadResult);
        String publicId = imageStorage.getPublicId(uploadResult);
        String assetId = imageStorage.getAssetId(uploadResult);

        productImage.setUrl(secureUrl);
        productImage.setPublicId(publicId);
        productImage.setAssetId(assetId);
        if (sortOrder != null) {
            productImage.setSortOrder(sortOrder);
        }
        if (isPrimary != null) {
            productImage.setIsPrimary(isPrimary);
        }

        ProductImage saved = productImageRepository.save(productImage);
        return mapToImageResponse(saved);
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#productId)")
    public void setPrimaryImage(Long productId, Long imageId) {
        lockEditable(productId);
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        
        if (!productImage.getProductId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }

        productImageRepository.findPrimaryByProductId(productId)
            .filter(existing -> !existing.getId().equals(imageId))
            .ifPresent(existingPrimary -> {
                existingPrimary.setIsPrimary(false);
                productImageRepository.save(existingPrimary);
            });

        productImage.setIsPrimary(true);
        productImageRepository.save(productImage);
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#productId)")
    public void reorderImages(Long productId, List<Long> imageIdsInOrder) {
        lockEditable(productId);
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        
        if (imageIdsInOrder == null
                || new java.util.HashSet<>(imageIdsInOrder).size() != imageIdsInOrder.size()
                || !imageIdsInOrder.stream().allMatch(id -> images.stream().anyMatch(img -> img.getId().equals(id))))
            throw new BadRequestException("Image IDs must be unique and belong to this product");
        for (int i = 0; i < imageIdsInOrder.size(); i++) {
            final int sortOrder = i;
            Long imageId = imageIdsInOrder.get(i);
            images.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .ifPresent(img -> {
                    img.setSortOrder(sortOrder);
                    productImageRepository.save(img);
                });
        }
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#id)")
    public ProductResponse updateSellerProductStatus(Long id, ProductStatus status) {
        Product product = lockEditable(id);
        if (visibility.canModerate()) {
            validateAdminStatus(product, status);
            product.setStatus(status);
            return mapToResponse(productRepository.save(product));
        }
        ProductStatus current = product.getStatus();
        boolean valid = (status == ProductStatus.UNLISTED && current == ProductStatus.ACTIVE)
                || (status == ProductStatus.ARCHIVED && (current == ProductStatus.ACTIVE
                    || current == ProductStatus.UNLISTED || current == ProductStatus.PENDING_APPROVAL
                    || current == ProductStatus.REJECTED))
                || (status == ProductStatus.PENDING_APPROVAL
                    && (current == ProductStatus.UNLISTED || current == ProductStatus.ARCHIVED));
        if (!valid) throw new BadRequestException("Invalid seller product status transition");
        product.setStatus(status);
        product.setStatusReason(null);
        product.setStatusChangedAt(LocalDateTime.now());
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        product.setStatusChangedBy(auth != null && auth.getPrincipal() instanceof Long actor ? actor : null);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@resourceAuthorization.admin()")
    public ProductResponse updateProductStatus(Long id, ProductStatus status) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        validateAdminStatus(product, status);
        product.setStatus(status);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@resourceAuthorization.admin()")
    public ProductResponse updateProductStatusWithReason(Long id, ProductStatus status, String reason, Long adminId) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        validateAdminStatus(product, status);
        product.setStatus(status);
        product.setStatusReason(reason);
        product.setStatusChangedAt(LocalDateTime.now());
        product.setStatusChangedBy(adminId);
        return mapToResponse(productRepository.save(product));
    }

    public Page<ProductResponse> getFilteredProducts(ProductStatus status, Long categoryId, Long sellerId, String county, String search, Pageable pageable) {
        // Note: county filter not implemented in repository query yet
        return productRepository.findFilteredProducts(status, categoryId, sellerId, search, pageable)
                .map(this::mapToResponse);
    }

    public Map<String, Object> getProductStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", productRepository.count());
        stats.put("live", productRepository.countByStatus(ProductStatus.ACTIVE));
        stats.put("pending", productRepository.countByStatus(ProductStatus.PENDING_APPROVAL));

        // Count products that need restock (stock <= lowStockThreshold or stock < 10 if threshold not set)
        stats.put("needsRestock", productRepository.countNeedsRestock());

        // Calculate live stock value (sum of price * stock for active products)
        BigDecimal liveStockValue = productRepository.calculateLiveStockValue();
        stats.put("liveStockValue", liveStockValue != null ? liveStockValue : BigDecimal.ZERO);

        return stats;
    }

    @Transactional(rollbackFor = IOException.class)
    @PreAuthorize("@productVisibility.canModifyProduct(#id)")
    public void deleteProduct(Long id) throws IOException {
        Product product = lockEditable(id);
        if (product.getStatus() != ProductStatus.ARCHIVED)
            throw new BadRequestException("Archive the product before deleting it");
        if (product.getReservedQty() != null && product.getReservedQty() > 0)
            throw new BadRequestException("Product has reserved stock and cannot be deleted");
        deleteAllProductImages(id);
        product.setStatus(ProductStatus.DELETED);
        product.setStatusChangedAt(LocalDateTime.now());
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        product.setStatusChangedBy(auth != null && auth.getPrincipal() instanceof Long actor ? actor : null);
        productRepository.save(product);
    }

    private void uploadImages(Long productId, List<MultipartFile> images) throws IOException {
        if (productImageRepository.findByProductId(productId).size() + images.size() > MAX_PRODUCT_IMAGES)
            throw new BadRequestException("At most " + MAX_PRODUCT_IMAGES + " product images are allowed");
        images.forEach(this::validateImage);
        int offset = productImageRepository.findByProductId(productId).size();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            if (!image.isEmpty()) {
                boolean isPrimary = (offset == 0 && i == 0);
                uploadProductImage(productId, image, offset + i, isPrimary);
            }
        }
    }

    private void deleteAllProductImages(Long productId) throws IOException {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        for (ProductImage image : images) {
            deleteStoredImage(image.getPublicId());
        }
        productImageRepository.deleteAll(images);
    }

    private static final int MAX_PRODUCT_IMAGES = 10;

    private void requireNotDeleted(Product product) {
        if (product.getStatus() == ProductStatus.DELETED)
            throw new ResourceNotFoundException("Product", product.getId());
    }

    private Product lockEditable(Long id) {
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        requireNotDeleted(product);
        return product;
    }

    private void validateAdminStatus(Product product, ProductStatus status) {
        requireNotDeleted(product);
        if (status == null || status == ProductStatus.DELETED)
            throw new BadRequestException("Use archived product deletion for terminal removal");
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 10L * 1024 * 1024)
            throw new BadRequestException("A nonempty image of at most 10 MiB is required");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(java.util.Locale.ROOT);
        try (var stream = file.getInputStream()) {
            byte[] h = stream.readNBytes(12);
            boolean valid = switch (type) {
                case "image/jpeg" -> h.length >= 3 && (h[0] & 255) == 255 && (h[1] & 255) == 216 && (h[2] & 255) == 255;
                case "image/png" -> h.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(h, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
                case "image/gif" -> h.length >= 6 && (new String(h, 0, 6, java.nio.charset.StandardCharsets.US_ASCII).matches("GIF8[79]a"));
                case "image/webp" -> h.length >= 12 && new String(h, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(h, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
                default -> false;
            };
            if (!valid) throw new BadRequestException("Image content must match JPEG, PNG, GIF or WebP type");
        } catch (IOException ex) {
            throw new BadRequestException("Cannot read image file");
        }
    }

    private Map<String, Object> uploadTracked(MultipartFile image, String folder) throws IOException {
        Map<String, Object> result = imageStorage.uploadImage(image, folder);
        if (imageStorage instanceof com.kilivana.backend.common.service.LocalImageStorage
                && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            String publicId = imageStorage.getPublicId(result);
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) imageStorage.deleteImage(publicId);
                        }
                    });
        }
        return result;
    }

    private void deleteStoredImage(String publicId) {
        if (imageStorage instanceof com.kilivana.backend.common.service.LocalImageStorage
                && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { imageStorage.deleteImage(publicId); }
                    });
        } else {
            // PostgreSQL image rows participate in the same transaction as product metadata.
            imageStorage.deleteImage(publicId);
        }
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImageResponse> imageResponses = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());

        // Get seller info for read-only fields
        String sellerName = null;
        String sellerPhone = null;
        String sellerCounty = null;
        try {
            UserResponse seller = userService.getUserById(product.getSellerId());
            sellerName = seller.getName();
            sellerPhone = seller.getPhone();
            sellerCounty = seller.getRegion();
        } catch (Exception e) {
            // Seller might not exist, ignore
        }

        // Get category name
        String categoryName = null;
        try {
            Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
            categoryName = category != null ? category.getName() : null;
        } catch (Exception e) {
            // Category might not exist, ignore
        }

        // Calculate isLive
        boolean isLive = product.getStatus() == ProductStatus.ACTIVE && product.getStockQty() > 0;

        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .sellerType(product.getSellerType())
                .categoryId(product.getCategoryId())
                .name(product.getName())
                .description(product.getDescription())
                .unit(product.getUnit())
                .price(product.getPrice())
                .stockQty(product.getStockQty())
                .reservedQty(product.getReservedQty())
                .soldQty(product.getSoldQty())
                .minimumOrderQty(product.getMinimumOrderQty())
                .status(product.getStatus())
                .qualityGrade(product.getQualityGrade())
                .harvestDate(product.getHarvestDate())
                .lowStockThreshold(product.getLowStockThreshold())
                .farmId(product.getFarmId())
                .statusReason(product.getStatusReason())
                .statusChangedAt(product.getStatusChangedAt())
                .statusChangedBy(product.getStatusChangedBy())
                .verifiedAt(product.getVerifiedAt())
                .verifiedBy(product.getVerifiedBy())
                .verifiedQty(product.getVerifiedQty())
                .lastInspectionId(product.getLastInspectionId())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .images(imageResponses)
                .sellerName(sellerName)
                .sellerPhone(sellerPhone)
                .sellerCounty(sellerCounty)
                .categoryName(categoryName)
                .farmName(null) // TODO: Fetch from Farm entity when available
                .farmCertified(false) // TODO: Fetch from Farm entity when available
                .isLive(isLive)
                .build();
    }

    private ProductImageResponse mapToImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .publicId(image.getPublicId())
                .assetId(image.getAssetId())
                .sortOrder(image.getSortOrder())
                .isPrimary(image.getIsPrimary())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
