package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.MovementHistory;
import com.enterprise.wms.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Spring Data repository for {@link MovementHistory} (stock movement audit trail). */
public interface MovementHistoryRepository extends JpaRepository<MovementHistory, Long> {

    /** Paginated listing of all movements. */
    Page<MovementHistory> findAll(Pageable pageable);

    /** Movements that occurred after a given timestamp (for velocity analysis). */
    List<MovementHistory> findByEventTimeAfter(LocalDateTime eventTime);

    /** All movements for a specific product. */
    List<MovementHistory> findByProduct(Product product);
}
