package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link SalesOrder} headers (basic CRUD only). */
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
}
