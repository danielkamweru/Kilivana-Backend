package com.kilivana.backend.ecommerce.service;

import com.kilivana.backend.ecommerce.dto.ProductRequest;
import com.kilivana.backend.ecommerce.dto.ProductResponse;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import com.kilivana.backend.common.dto.ApiResponse;
import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.exception.BadRequestException;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .sellerId(request.getSellerId())
                .sellerType(request.getSellerType())
                .categoryId(request.getCategoryId())
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

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return mapToResponse(product);
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
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setUnit(request.getUnit());
        product.setPrice(request.getPrice());
        product.setStockQty(request.getStockQty());
        product.setMinimumOrderQty(request.getMinimumOrderQty());
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setStatus(status);
        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
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
                .build();
    }
}
