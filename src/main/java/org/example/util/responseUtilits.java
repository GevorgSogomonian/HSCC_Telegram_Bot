package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;



@Service
@Slf4j
public class responseUtilits {
//    private void sendMessageResponse(SendMessage sendMessage) {
//        if (sendMessage.getChatId().isEmpty()) {
//            log.error(String.format("""
//                    ChatId is empty. ChatId: ->%s<-""",
//                    sendMessage.getChatId()));
//        }
//        try {
//            execute(sendMessage);
//        } catch (TelegramApiException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void sendSplitTextResponse(Long chatId, String text) {
//        int maxMessageLength = 4096;
//        for (int i = 0; i < text.length(); i += maxMessageLength) {
//            String part = text.substring(i, Math.min(text.length(), i + maxMessageLength));
//            sendResponse(chatId, part);
//        }
//    }
//
//    private void sendResponse(Long chatId, String text) {
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId);
//        message.setText(text);
//
//        message.setParseMode("Markdown");
//        try {
//            execute(message);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }
}
