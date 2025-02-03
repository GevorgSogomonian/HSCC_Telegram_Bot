package org.example.repository;

import org.example.data_classes.data_base.comparison_tables.EventMissing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface EventMissingRepository extends JpaRepository<EventMissing, Long> {
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            delete from event_missing where event_id = ?1 and chat_id = ?2""")
    void removeEventMissingsByEventIdAndChatId(Long eventId, Long chatId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            delete from event_missing where event_id = ?1""")
    void removeEventMissingsByEventId(Long eventId);
}
