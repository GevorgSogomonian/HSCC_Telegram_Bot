package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.repository.EventRepository;
import org.example.telegram.api.TelegramSender;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
public class StringValidator {

    private final TelegramSender telegramSender;
    private final EventRepository eventRepository;

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

    public String validateEventName(Long chatId, String eventName) {
        int maxNameLength = 120;

        if (eventName.isBlank()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                    Название мероприятия не может быть пустым(""")
                    .build());
            return "";
        } else if (eventName.length() > maxNameLength) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Название мероприятия не может быть больше %s символов(""",
                            maxNameLength))
                    .build());
            return "";
        }

        return eventName;
    }

    public String validateEventDescription(Long chatId, String eventDescription) {
        int maxDescriptioonLength = 2000;

        if (eventDescription.isBlank()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                    Описание мероприятия не может быть пустым(""")
                    .build());
            return "";
        } else if (eventDescription.length() > maxDescriptioonLength) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Описание мероприятия не может быть больше %s символов(""",
                            maxDescriptioonLength))
                    .build());
            return "";
        }

        return eventDescription;
    }

    public LocalDateTime validateEventStartTime(Long chatId, String eventStartTime) {
        try {
            return LocalDateTime.parse(eventStartTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException e) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Некорректный формат даты. Пожалуйста, введите дату в формате:
                                    `YYYY-MM-DD HH:mm`""")
                    .build());

            return null;
        }
    }
}
