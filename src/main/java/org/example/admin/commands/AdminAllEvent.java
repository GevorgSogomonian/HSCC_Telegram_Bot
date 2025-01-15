package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.image.ImageService;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.InputStream;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AdminAllEvent {
    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final AdminStart adminStart;
    private final ImageService imageService;

    public void handleAllEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.findAll();

        if (allEvents.isEmpty()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятий нет.""")
                    .build());
            adminStart.handleStartState(update);

            return;
        }

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();

//            InputStream fileStream = null;
//            try {
//                fileStream = imageService.getFile("pictures", event.getImageUrl());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//
//            InputFile inputFile = new InputFile(fileStream, event.getImageUrl());
            sendPhoto.setChatId(chatId.toString());
//            sendPhoto.setPhoto(inputFile);

            sendPhoto.setCaption(event.toString());
//            sendPhoto.setCaption(String.format("""
//                    *%s*:
//
//                    %s""",
//                    event.getEventName(), event.getDescription()));

            InlineKeyboardButton editButton = new InlineKeyboardButton("редактировать");
            editButton.setCallbackData("edit_event_" + event.getId());

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("удалить");
            deleteButton.setCallbackData("delete_event_" + event.getId());


            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(editButton))
                    .keyboardRow(List.of(deleteButton))
                    .build();

            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            telegramSender.sendPhoto(chatId, event.getId(), sendPhoto);
            log.info("try to send image from minio. Image Name");
        });
    }
}
