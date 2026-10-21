package com.kilivana.backend.security;

import com.kilivana.backend.common.enums.ProductStatus;
import com.kilivana.backend.common.exception.ResourceNotFoundException;
import com.kilivana.backend.ecommerce.entity.Product;
import com.kilivana.backend.ecommerce.repository.ProductImageRepository;
import com.kilivana.backend.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** ACTIVE is the canonical approved status. Stock and moderation transitions are unchanged. */
@Component("productVisibility")
@RequiredArgsConstructor
public class ProductVisibility {
    private final ResourceAuthorization authorization;
    private final ProductRepository products;
    private final ProductImageRepository images;

    public boolean canRead(Product product) {
        return product.getStatus() != ProductStatus.DELETED &&
                (product.getStatus() == ProductStatus.ACTIVE || authorization.seller(product.getSellerId()));
    }

    public boolean canModerate() { return authorization.admin(); }

    public boolean canModifyProduct(Long id) {
        return products.findById(id).map(p -> authorization.seller(p.getSellerId())).orElse(false);
    }

    public boolean canReviewSeller(Long sellerId) {
        return authorization.seller(sellerId);
    }

    public void requireReadable(Product product) {
        if (!canRead(product)) throw new ResourceNotFoundException("Product", product.getId());
    }

    /** Check links as well as folder hints, so old URLs cannot bypass the catalogue policy. */
    public void requireImageReadable(String publicId, boolean productFolder) {
        var links = images.findAllByPublicId(publicId);
        if (links.isEmpty() && productFolder) throw new ResourceNotFoundException("Image not found");
        for (var link : links) {
            Product product = products.findById(link.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
            requireReadable(product);
        }
    }
}
