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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorage cloudinaryService;
    private final UserService userService;

    @Transactional
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
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name(name)
                            .type(sellerType)
                            .active(true)
                            .build()))
                    .getId();
        }
        throw new BadRequestException("Category ID or category name is required");
    }

    @Transactional(readOnly = true)
    public void ensureProductOwnership(Long productId, Long userId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        UserResponse user = userService.getUserById(userId);
        if (!product.getSellerId().equals(userId) && !user.getRole().isStaff()) {
            throw new BadRequestException("You do not have permission to modify this product");
        }
    }

    @Transactional
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
        return mapToResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<ProductResponse> searchProducts(String name, Long categoryId, String sellerType, Pageable pageable) {
        return productRepository.searchProducts(name, categoryId, sellerType, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (request.getCategoryId() != null
                || (request.getCategory() != null && !request.getCategory().isBlank())) {
            product.setCategoryId(resolveCategoryId(request, product.getSellerType()));
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setUnit(request.getUnit());
        product.setPrice(request.getPrice());
        product.setStockQty(request.getStockQty());
        product.setMinimumOrderQty(request.getMinimumOrderQty());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProductWithImages(Long id, ProductRequest request, List<MultipartFile> images) throws IOException {
        ProductResponse productResponse = updateProduct(id, request);
        
        if (images != null && !images.isEmpty()) {
            deleteAllProductImages(id);
            uploadImages(id, images);
            productResponse = getProductById(id);
        }
        
        return productResponse;
    }

    @Transactional
    public ProductImageResponse uploadProductImage(Long productId, MultipartFile image, Integer sortOrder, Boolean isPrimary) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        if (Boolean.TRUE.equals(isPrimary)) {
            productImageRepository.findPrimaryByProductId(productId)
                .ifPresent(existingPrimary -> {
                    existingPrimary.setIsPrimary(false);
                    productImageRepository.save(existingPrimary);
                });
        }

        String folder = "kilivana/products/" + productId;
        Map<String, Object> uploadResult = cloudinaryService.uploadImage(image, folder);
        
        String secureUrl = cloudinaryService.getSecureUrl(uploadResult);
        String publicId = cloudinaryService.getPublicId(uploadResult);
        String assetId = cloudinaryService.getAssetId(uploadResult);

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

    @Transactional
    public void deleteProductImage(Long productId, Long imageId) throws IOException {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageId));
        
        if (!productImage.getProductId().equals(productId)) {
            throw new BadRequestException("Image does not belong to this product");
        }
        
        cloudinaryService.deleteImage(productImage.getPublicId());
        productImageRepository.delete(productImage);
    }

    @Transactional
    public ProductImageResponse updateProductImage(Long productId, Long imageId, MultipartFile newImage, Integer sortOrder, Boolean isPrimary) throws IOException {
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
        Map<String, Object> uploadResult = cloudinaryService.replaceImage(productImage.getPublicId(), newImage, folder);
        
        String secureUrl = cloudinaryService.getSecureUrl(uploadResult);
        String publicId = cloudinaryService.getPublicId(uploadResult);
        String assetId = cloudinaryService.getAssetId(uploadResult);

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

    @Transactional
    public void setPrimaryImage(Long productId, Long imageId) {
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

    @Transactional
    public void reorderImages(Long productId, List<Long> imageIdsInOrder) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        
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

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setStatus(status);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) throws IOException {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        
        deleteAllProductImages(id);
        productRepository.deleteById(id);
    }

    private void uploadImages(Long productId, List<MultipartFile> images) throws IOException {
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            if (!image.isEmpty()) {
                boolean isPrimary = (i == 0);
                uploadProductImage(productId, image, i, isPrimary);
            }
        }
    }

    private void deleteAllProductImages(Long productId) throws IOException {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        for (ProductImage image : images) {
            cloudinaryService.deleteImage(image.getPublicId());
        }
        productImageRepository.deleteAll(images);
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImageResponse> imageResponses = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId())
                .stream()
                .map(this::mapToImageResponse)
                .collect(Collectors.toList());

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
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .images(imageResponses)
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
