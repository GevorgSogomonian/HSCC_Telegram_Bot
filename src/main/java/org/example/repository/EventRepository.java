package org.example.repository;

import org.example.entity.Event;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventName(String eventName);

    @NotNull Optional<Event> findById(@NotNull Long id);
}
