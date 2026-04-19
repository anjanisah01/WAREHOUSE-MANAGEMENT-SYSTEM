package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.Location;
import com.enterprise.wms.domain.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data repository for {@link Location} (warehouse storage slots). */
public interface LocationRepository extends JpaRepository<Location, Long> {

    /** Find locations in a warehouse that still have space (occupied < threshold). */
    List<Location> findByWarehouseAndOccupiedLessThan(Warehouse warehouse, Integer occupied);
}
