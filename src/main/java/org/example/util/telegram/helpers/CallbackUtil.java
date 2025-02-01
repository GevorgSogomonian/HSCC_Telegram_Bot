package org.example.util.telegram.helpers;

import lombok.RequiredArgsConstructor;
import org.example.util.telegram.api.TelegramSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class CallbackUtil {
    private final TelegramSender telegramSender;

    public void answerCallback(Long chatId, String callbackQueryId) {
        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());
    }

    public void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }
}
