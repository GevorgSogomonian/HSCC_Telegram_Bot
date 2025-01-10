package org.example.repository;

import org.example.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventName(String eventName);

    Optional<Event> findFirstByCreatorChatIdOrderByUpdatedAtDesc(Long creatorChatId);
}
