package org.example.repository;

import org.example.data_classes.data_base.comparison_tables.EventVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EventVisitRepository extends JpaRepository<EventVisit, Long> {
    @Query(nativeQuery = true, value = """
            select event_visit.chat_id
            from event_visit
            where event_id = ?1""")
    List<Long> getVisitorsChatIds(Long eventId);

    @Query(nativeQuery = true, value = """
            select count(*)
            from event_visit
            where event_id = ?1""")
    Long getVisitorsCount(Long eventId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            delete from event_visit where event_id = ?1""")
    void removeEventVisitsByEventId(Long eventId);

    boolean existsByChatIdAndEventId(Long chatId, Long eventId);
}
