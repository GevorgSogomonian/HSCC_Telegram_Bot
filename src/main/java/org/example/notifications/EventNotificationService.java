package org.example.notifications;

import lombok.RequiredArgsConstructor;
import org.example.entity.EventNotification;
import org.example.repository.EventNotificationRepository;
import org.example.repository.EventSubscriptionRepository;
import org.example.telegram.api.TelegramSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventNotificationService {
    private final EventNotificationRepository eventNotificationRepository;
    private final TelegramSender telegramSender;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    public void saveNotification(EventNotification eventNotification) {
        eventNotificationRepository.save(eventNotification);
    }

    @Scheduled(fixedRate = 30_000)
    public void eventChecker() {
        List<EventNotification> eventNotificationList = eventNotificationRepository.getActualNotifications();
        if (!eventNotificationList.isEmpty()) {
            eventNotificationList.forEach(eventNotification -> {
                List<Long> subscriberChatIds = eventSubscriptionRepository.getSubscribersChatIds(eventNotification.getEventId());
                if (!subscriberChatIds.isEmpty()) {
                    subscriberChatIds.forEach(chatId ->
                        telegramSender.sendText(chatId, SendMessage.builder()
                                .chatId(chatId)
                                .text(eventNotification.getNotificationText())
                                .build()));
                    eventNotificationRepository.delete(eventNotification);
                }
            });
        }
    }
}
