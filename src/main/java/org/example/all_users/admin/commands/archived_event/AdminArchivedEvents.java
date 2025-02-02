package org.example.all_users.admin.commands.archived_event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.data_classes.data_base.entity.Event;
import org.example.repository.EventRepository;
import org.example.repository.EventVisitRepository;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminArchivedEvents {
    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final EventVisitRepository eventVisitRepository;

    public void handleArchivedEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.getArchivedEvents();

        if (allEvents.isEmpty()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            В архиве мероприятий нет.""")
                    .build());
            return;
        }

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            Long visitorsCount = eventVisitRepository.getVisitorsCount(event.getId());

            sendPhoto.setCaption(String.format("""
                    %s
                    Пришедших: *%s*""", event, visitorsCount));

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("удалить");
            deleteButton.setCallbackData("delete_offer-deleting-event_" + event.getId());

            InlineKeyboardButton messageButton = new InlineKeyboardButton("сообщение пришедшим");
            messageButton.setCallbackData("message-to-visitors_to-event-visitors_" + event.getId());

            InlineKeyboardButton statisticButton = new InlineKeyboardButton("статистика");
            statisticButton.setCallbackData("statistic_archived-event_" + event.getId());

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(deleteButton, statisticButton))
                    .keyboardRow(List.of(messageButton))
                    .build();

            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            telegramSender.sendPhoto(chatId, event.getId(), sendPhoto);
            log.info("try to send image from minio. Image Name");
        });
    }
}
