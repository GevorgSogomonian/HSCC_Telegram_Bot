package org.example.repository;

import org.example.data_classes.data_base.entity.EventNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EventNotificationRepository extends JpaRepository<EventNotification, Long> {
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM event_notification n
            WHERE n.notification_time < CURRENT_TIMESTAMP""")
    List<EventNotification> getActualNotifications();

    boolean existsByEventId(Long eventId);

    @Modifying
    @Transactional
    void removeByEventId(Long eventId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            delete
            from event_notification
            where event_id = ?1""")
    void deleteNotification(Long eventId);
}
