package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.SalesOrder;
import com.enterprise.wms.domain.entity.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data repository for {@link SalesOrderLine} (order line items). */
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {

    /** All line items belonging to a given order. */
    List<SalesOrderLine> findByOrderRef(SalesOrder order);
}
