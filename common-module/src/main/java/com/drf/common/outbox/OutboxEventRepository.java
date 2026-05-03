package com.drf.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "SELECT * FROM outbox_event WHERE status = 'PENDING' ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findPendingWithLock(@Param("limit") int limit);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :status WHERE e.eventId IN :ids")
    void bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") OutboxStatus status);
}
