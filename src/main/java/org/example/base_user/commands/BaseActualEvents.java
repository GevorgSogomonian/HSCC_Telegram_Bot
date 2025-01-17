package org.example.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.entity.Event;
import org.example.entity.Usr;
import org.example.repository.EventRepository;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BaseActualEvents {

    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;

    public void handleActualEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.findAll();
        Optional<Usr> userOptional = updateUtil.getUser(update);

        if (userOptional.isPresent() && !allEvents.isEmpty()) {
            Usr user = userOptional.get();

            allEvents.forEach(event -> {
                if (!user.getSubscribedEventIds().contains(event.getId().toString())) {
                    SendPhoto sendPhoto = new SendPhoto();
                    sendPhoto.setCaption(event.toString());
                    sendPhoto.setChatId(chatId.toString());

                    InlineKeyboardButton button = new InlineKeyboardButton("Подписаться");
                    button.setCallbackData("subscribe_offer-subscribe-to-event_" + event.getId());

                    InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                            .clearKeyboard()
                            .keyboardRow(List.of(button))
                            .build();

                    sendPhoto.setReplyMarkup(keyboardMarkup);

                    telegramSender.sendPhoto(chatId, event.getId(), sendPhoto);
                }
            });

            String userSubscribedEventIds = user.getSubscribedEventIds();

            if (!userSubscribedEventIds.isBlank()) {
                List<Event> subscribedEvents = new ArrayList<>();
                for (String eventId : userSubscribedEventIds.split("_")) {
                    if (!eventId.isBlank()) {
                        Optional<Event> eventOptional = eventRepository.findById(Long.parseLong(eventId));
                        eventOptional.ifPresent(subscribedEvents::add);
                    }
                }

                if (!subscribedEvents.isEmpty()) {
                    for (Event event : subscribedEvents) {
                        SendPhoto sendPhoto = new SendPhoto();
                        sendPhoto.setCaption(event.toString());
                        sendPhoto.setChatId(chatId.toString());

                        InlineKeyboardButton button = new InlineKeyboardButton("Отписаться");
                        button.setCallbackData("unsubscribe_offer-unsubscribe-from-event_" + event.getId());

                        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                                .clearKeyboard()
                                .keyboardRow(List.of(button))
                                .build();

                        sendPhoto.setReplyMarkup(keyboardMarkup);

                        telegramSender.sendPhoto(chatId, event.getId(), sendPhoto);
                    }
                }
            }
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Сейчас нет актуальных мероприятий.""")
                    .build());
        }
    }
}
