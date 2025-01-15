package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.image.ImageService;
import org.example.telegram.api.TelegramSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminDeleteEvent {
    private final EventRepository eventRepository;
    private final ImageService imageService;
    private final TelegramSender telegramSender;

    public void handleDeleteEvent(Long chatId, Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            String eventName = event.getEventName();

            String imageUrl = event.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                String bucketName = "pictures";
                imageService.deleteImage(bucketName, imageUrl);
            }

            eventRepository.delete(event);

            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text(String.format("""
                    Мероприятие *%s* удалено!""", eventName))
                    .build());
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятие не найдено!""")
                    .build());
        }
    }
}
