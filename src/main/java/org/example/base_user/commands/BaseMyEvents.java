package org.example.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.entity.Event;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.repository.EventRepository;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaseMyEvents {
    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;

    public void handleMyEventsCommand(Update update) {
        System.out.println("мои мероприятия старт");
        Long chatId = updateUtil.getChatId(update);
        Optional<Usr> userOptional = updateUtil.getUser(update);

        if (userOptional.isPresent()) {
            Usr user = userOptional.get();
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
                        sendPhoto.setCaption(String.format("""
                                        *%s*:
                                        
                                        %s""",
                                event.getEventName(), event.getDescription()));
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
                } else {
                    telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Вы не подписаны ни на одно мероприятие.""")
                            .build());
                }

            } else {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                У вас нет активных подписок на мероприятия.""")
                        .build());
            }
        }
    }
}