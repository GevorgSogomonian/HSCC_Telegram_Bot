package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.repository.UserRepository;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminAllEvent {
    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final UserRepository userRepository;

    public void handleAllEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.findAll();
        List<String> allUsersSubscribeEventIds = userRepository.getAllsubscribedEventIds();
        Map<Long, Integer> eventSubscribersMap = new HashMap<>();

        if (allEvents.isEmpty()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятий нет.""")
                    .build());
            return;
        }

        for (Event event : allEvents) {
            eventSubscribersMap.put(event.getId(), 0);
        }

        System.out.println(eventSubscribersMap);


        for (String eventIds : allUsersSubscribeEventIds) {
            String[] eventIdsArray = eventIds.split("_");
            for (String eventIdString : eventIdsArray) {
                if (eventIdString != null && !eventIdString.isBlank()) {
                    Long eventIdLong = Long.parseLong(eventIdString);
                    if (eventSubscribersMap.containsKey(eventIdLong)) {
                        eventSubscribersMap.put(eventIdLong, eventSubscribersMap.get(eventIdLong) + 1);
                    }
                }
            }
        }

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());

            sendPhoto.setCaption(String.format("""
                    %s
                    Подписчиков: *%s*""", event.toString(), eventSubscribersMap.get(event.getId())));

            InlineKeyboardButton editButton = new InlineKeyboardButton("редактировать");
            editButton.setCallbackData("edit_offer-editing-event_" + event.getId());

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("удалить");
            deleteButton.setCallbackData("delete_offer-deleting-event_" + event.getId());

            InlineKeyboardButton registrateButton = new InlineKeyboardButton("отметить пришедших");
            registrateButton.setCallbackData("register-users");

            InlineKeyboardButton messageButton = new InlineKeyboardButton("сообщение подписчикам");
            messageButton.setCallbackData("message-to-subscribers_to-event-subscribers_" + event.getId());

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(editButton, deleteButton))
                    .keyboardRow(List.of(registrateButton))
                    .keyboardRow(List.of(messageButton))
                    .build();

            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            telegramSender.sendPhoto(chatId, event.getId(), sendPhoto);
            log.info("try to send image from minio. Image Name");
        });
    }
}
