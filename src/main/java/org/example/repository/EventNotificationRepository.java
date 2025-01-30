package org.example.repository;

import org.example.entity.EventNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventNotificationRepository extends JpaRepository<EventNotification, Long> {
    @Query("""
            SELECT n
            FROM EventNotification n
            WHERE n.notificationTime > CURRENT_TIMESTAMP""")
    Optional<EventNotification> getActualNotification();
}
