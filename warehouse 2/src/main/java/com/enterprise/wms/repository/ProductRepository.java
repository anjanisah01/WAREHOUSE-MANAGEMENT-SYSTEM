package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data repository for {@link Product} (SKU catalogue). */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Look up a product by its barcode (used during goods receipt scanning). */
    Optional<Product> findByBarcode(String barcode);

    /** Look up a product by its SKU code. */
    Optional<Product> findBySku(String sku);
}
