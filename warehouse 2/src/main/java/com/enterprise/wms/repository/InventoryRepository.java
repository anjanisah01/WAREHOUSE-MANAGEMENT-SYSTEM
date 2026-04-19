package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.Inventory;
import com.enterprise.wms.domain.entity.LotBatch;
import com.enterprise.wms.domain.entity.Product;
import com.enterprise.wms.domain.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link Inventory} records.
 * Supports lookups by product, warehouse, lot/batch, and stock availability.
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** Paginated listing of all inventory rows. */
    Page<Inventory> findAll(Pageable pageable);

    /** All stock rows in a specific warehouse. */
    List<Inventory> findByWarehouse(Warehouse warehouse);

    /** All stock rows for a specific product (across warehouses). */
    List<Inventory> findByProduct(Product product);

    /** Unique stock row for a product in a warehouse (ignoring lot). */
    Optional<Inventory> findByProductAndWarehouse(Product product, Warehouse warehouse);

    /** Stock rows with available quantity > threshold (used for FEFO allocation). */
    List<Inventory> findByProductAndWarehouseAndQuantityGreaterThan(Product product, Warehouse warehouse, Integer quantity);

    /** Exact match on product + warehouse + lot/batch. */
    Optional<Inventory> findByProductAndWarehouseAndLotBatch(Product product, Warehouse warehouse, LotBatch lotBatch);
}
