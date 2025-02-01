package org.example.repository;

import org.example.data_classes.data_base.entity.EventDestructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventDestructorRepository extends JpaRepository<EventDestructor, Long> {

    @Query(nativeQuery = true, value = """
                        select *
                                                from event_destructor
                                                where destruction_time < CURRENT_TIMESTAMP;
                        """)
    List<EventDestructor> getActualEventDestructors();

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            update event_destructor
            set destruction_time = ?2
            where event_id = ?1""")
    void updateTime(Long eventId, LocalDateTime newNotificationTime);
}
