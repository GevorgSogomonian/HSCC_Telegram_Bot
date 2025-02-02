package org.example.util.telegram.api;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.dto.ChatBotResponse;
import org.example.util.image.ImageService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;

@Service
@RequiredArgsConstructor
public class TelegramSender {
    private final TelegramApiQueue telegramApiQueue;

    public void sendPhoto(Long chatId, SendPhoto sendPhoto) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendPhoto));
    }

    public void sendPhoto(Long chatId, Long eventId, SendPhoto sendPhoto) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendPhoto, eventId));
    }

    public void sendText(Long chatId, SendMessage sendMessage) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendMessage));
    }

    public void deleteMessage(Long chatId, DeleteMessage deleteMessage) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, deleteMessage));
    }

    public void answerCallbackQuerry(Long chatId, AnswerCallbackQuery answerCallbackQuery) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, answerCallbackQuery));
    }

    public void forwardMessage(Long chatId, ForwardMessage forwardMessage) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, forwardMessage));
    }

    public void sendDocument(Long chatId, SendDocument sendDocument) {
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendDocument));
    }
}
