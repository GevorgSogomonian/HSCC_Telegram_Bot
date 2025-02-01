package org.example.all_users.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.Event;
import org.example.data_classes.data_base.entity.Usr;
import org.example.repository.EventRepository;
import org.example.repository.EventSubscriptionRepository;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BaseActualEvents {

    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    public void handleActualEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.findAll();
        Optional<Usr> userOptional = updateUtil.getUser(update);
        List<Long> subscribedEventIds = eventSubscriptionRepository.getSubscribedEventIds(chatId);

        if (userOptional.isPresent() && !allEvents.isEmpty()) {

            allEvents.forEach(event -> {
                if (!subscribedEventIds.contains(event.getId())) {
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

            allEvents.forEach(event -> {
                if (subscribedEventIds.contains(event.getId())) {
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
            });
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Сейчас нет актуальных мероприятий.""")
                    .build());
        }
    }
}
