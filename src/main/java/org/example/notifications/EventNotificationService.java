package org.example.notifications;

import lombok.RequiredArgsConstructor;
import org.example.entity.Event;
import org.example.entity.EventNotification;
import org.example.repository.EventNotificationRepository;
import org.example.repository.EventRepository;
import org.example.telegram.api.TelegramSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventNotificationService {
    private final EventNotificationRepository eventNotificationRepository;
    private final TelegramSender telegramSender;
    private final EventRepository eventRepository;

    public void saveNotification(EventNotification eventNotification) {
        eventNotificationRepository.save(eventNotification);
    }

//    @Scheduled(fixedRate = 30_000)
//    public void eventChecker() {
//        List<EventNotification> eventNotificationList = eventNotificationRepository.findAll();
//        if (!eventNotificationList.isEmpty()) {
//            eventNotificationList.stream()
//                    .filter(eventNotification -> eventNotification.getNotificationTime().isBefore(LocalDateTime.now()))
//                    .forEach(eventNotification -> {
//                        Optional<Event> eventOptional = eventRepository.findById(eventNotification.getEventId());
//                        if (eventOptional.isPresent()) {
//                            Event event = eventOptional.get();
//                            telegramSender.sendText(event.get);
//                        }
//                        String subscribersChatId = eve
//                        telegramSender.sendText();
//                    });
////                    .toList();
//
//
//
//            System.out.println("Entity" + filteredList);
//        }
//    }
}
