package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data repository for {@link Warehouse} entities. */
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /** Look up a warehouse by its unique code (e.g. "WH-001"). */
    Optional<Warehouse> findByCode(String code);
}
