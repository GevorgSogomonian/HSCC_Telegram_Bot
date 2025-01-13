package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.telegram.api.TelegramSender;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class StringValidator {

    private final TelegramSender telegramSender;

    public String validateAndFormatFirstName(Long chatId, String name) {
        if (name == null || name.length() <= 1) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Имя должно содержать более одного символа.""")
                    .build());
            return "";
        } else if (name.matches(".*\\d.*")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Имя не должно содержать цифры.""")
                    .build());
            return "";
        } else if (!name.matches("[а-яА-Я]+")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Имя должно содержать только русские буквы.""")
                    .build());
            return "";
        } else if (!Character.isUpperCase(name.charAt(0))) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            return name;
        } else {
            return name;
        }
    }

    public String validateAndFormatLastName(Long chatId, String name) {
        if (name == null || name.length() <= 1) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Имя должно содержать более одного символа.""")
                    .build());
            return "";
        } else if (name.matches(".*\\d.*")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Имя не должно содержать цифры.""")
                    .build());
            return "";
        } else if (!name.matches("[а-яА-Я]+")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Имя должно содержать только русские буквы.""")
                    .build());
            return "";
        } else if (!Character.isUpperCase(name.charAt(0))) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            return name;
        } else {
            return name;
        }
    }
}
