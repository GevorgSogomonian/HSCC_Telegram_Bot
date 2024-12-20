package org.example.repository;

import org.example.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventName(String eventName);

//    Optional<Event> findEventByCreatorChatId(Long creatorChatId);

    Optional<Event> findFirstByCreatorChatIdOrderByUpdatedAtDesc(Long creatorChatId);
}
