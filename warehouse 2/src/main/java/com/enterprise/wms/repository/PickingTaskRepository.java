package com.enterprise.wms.repository;

import com.enterprise.wms.domain.entity.PickingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link PickingTask} entities. */
public interface PickingTaskRepository extends JpaRepository<PickingTask, Long> {

    /** Find a picking task by its primary key. */
    Optional<PickingTask> findById(Long id);

    /** All picking tasks, newest first (for the task list UI). */
    List<PickingTask> findAllByOrderByCreatedAtDesc();
}
