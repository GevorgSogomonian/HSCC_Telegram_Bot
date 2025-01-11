package org.example.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotResponse;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.util.image.ImageService;
import org.example.telegram.api.TelegramApiQueue;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BaseActualEvents {

    private final UpdateUtil updateUtil;
    private final EventRepository eventRepository;
    private final ImageService imageService;
    private final TelegramApiQueue telegramApiQueue;

    public void handleActualEventsCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        List<Event> allEvents = eventRepository.findAll();

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setCaption(String.format("""
                    *%s*:
                    
                    %s""",
                    event.getEventName(), event.getDescription()));
            InputStream fileStream = null;
            try {
                fileStream = imageService.getFile("pictures", event.getImageUrl());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            InputFile inputFile = new InputFile(fileStream, event.getImageUrl());
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(inputFile);

            InlineKeyboardButton button = new InlineKeyboardButton("Подписаться");
            button.setCallbackData("регистрация события в google calendar");

            InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(button))
                    .build();

            sendPhoto.setReplyMarkup(keyboardMarkup);

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendPhoto));
        });
    }
}
