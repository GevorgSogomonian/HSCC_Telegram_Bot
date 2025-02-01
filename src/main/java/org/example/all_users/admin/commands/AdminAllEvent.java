package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.data_classes.data_base.entity.Event;
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

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminAllEvent {
    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    public void handleAllEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.findAll();

        if (allEvents.isEmpty()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятий нет.""")
                    .build());
            return;
        }

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            Long subscribersCount = eventSubscriptionRepository.getSubscribersCountByEventId(event.getId());

            sendPhoto.setCaption(String.format("""
                    %s
                    Подписчиков: *%s*""", event, subscribersCount));

            InlineKeyboardButton editButton = new InlineKeyboardButton("редактировать");
            editButton.setCallbackData("edit_offer-editing-event_" + event.getId());

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("удалить");
            deleteButton.setCallbackData("delete_offer-deleting-event_" + event.getId());

            InlineKeyboardButton registrateButton = new InlineKeyboardButton("отметить пришедших");
            registrateButton.setCallbackData("visits_start-marking_" + event.getId());

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
