package org.example.telegram.api;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotResponse;
import org.example.util.image.ImageService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

@Service
@RequiredArgsConstructor
public class TelegramSender {
    private final TelegramApiQueue telegramApiQueue;
    private final ImageService imageService;

    public void sendPhoto(Long chatId, SendPhoto sendPhoto) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendPhoto));
    }

    public void sendText(Long chatId, SendMessage sendMessage) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendMessage));
    }
}
