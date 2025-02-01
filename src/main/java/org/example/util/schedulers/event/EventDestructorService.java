package org.example.util.schedulers.event;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.EventDestructor;
import org.example.repository.EventDestructorRepository;
import org.example.repository.EventSubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventDestructorService {

    private final EventDestructorRepository eventDestructorRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    @Scheduled(fixedRate = 3600_000)
    public void eventDestructorChecker() {
        List<EventDestructor> eventDestructorList = eventDestructorRepository.getActualEventDestructors();
        if (!eventDestructorList.isEmpty()) {
            eventDestructorList.forEach(eventDestructor -> {
                eventSubscriptionRepository.removeEventSubscriptionByEventId(eventDestructor.getEventId());
            });
        }
    }
}