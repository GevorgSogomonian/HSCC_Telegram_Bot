package org.example.repository;

import org.example.data_classes.data_base.entity.Event;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventName(String eventName);

    @NotNull Optional<Event> findById(@NotNull Long id);

    @Query(nativeQuery = true, value = """
                        select *
                        from event
                        where start_time + INTERVAL :duration HOUR > CURRENT_TIMESTAMP
                        """)
    List<Event> getActualEvents(@Param("duration") int duration);

    @Query(nativeQuery = true, value = """
                        select *
                        from event
                        where start_time + INTERVAL :duration HOUR < CURRENT_TIMESTAMP
                        """)
    List<Event> getArchivedEvents(@Param("duration") int duration);
}
