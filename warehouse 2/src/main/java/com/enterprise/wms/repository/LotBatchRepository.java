package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.LotBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/** Spring Data repository for {@link LotBatch} (lot/batch tracking). */
public interface LotBatchRepository extends JpaRepository<LotBatch, Long> {

    /** Find all batches expiring on or before a given date (for expiry alerts). */
    List<LotBatch> findByExpiryDateLessThanEqual(LocalDate date);
}
