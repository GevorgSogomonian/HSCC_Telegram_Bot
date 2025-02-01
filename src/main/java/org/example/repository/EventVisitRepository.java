package org.example.repository;

import org.example.data_classes.data_base.comparison_tables.EventVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EventVisitRepository extends JpaRepository<EventVisit, Long> {
    @Query(nativeQuery = true, value = """
            select event_subscription.event_id
            from event_subscription
            where chat_id = ?1""")
    List<Long> getSubscribedEventIds(Long chatId);

    @Query(nativeQuery = true, value = """
            select event_subscription.chat_id
            from event_subscription
            where event_id = ?1""")
    List<Long> getSubscribersChatIds(Long eventId);

    @Query(nativeQuery = true, value = """
            select count(*)
            from event_subscription
            where event_id = ?1""")
    Long getSubscribersCountByEventId(Long eventId);

    @Query(nativeQuery = true, value = """
            select count(*)
            from event_subscription
            where chat_id = ?1""")
    Long getSubscribersCountByChatId(Long chatId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            delete from event_subscription where event_id = ?1 and chat_id = ?2""")
    void removeEventSubscriptionByEventIdAndChatId(Long eventId, Long chatId);

    boolean existsByChatIdAndEventId(Long chatId, Long eventId);
}
