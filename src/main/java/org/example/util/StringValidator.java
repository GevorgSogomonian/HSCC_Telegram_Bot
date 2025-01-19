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

    public String validateAndFormatFirstName(Long chatId, String name) {
        if (name == null || name.length() <= 1) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Имя должно содержать более одного символа.""")
                    .build());
        } else if (name.matches(".*\\d.*")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Имя не должно содержать цифры.""")
                    .build());
        } else if (name.contains(" ")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Введите только имя, без пробелов.""")
                    .build());
        } else if (!name.matches("[а-яА-Я]+")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Имя должно содержать только русские буквы.""")
                    .build());
        } else if (!Character.isUpperCase(name.charAt(0))) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            return name;
        } else {
            return name;
        }

        return null;
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
        int maxDescriptionLength = 840;

        if (eventDescription.isBlank()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                    Описание мероприятия не может быть пустым(""")
                    .build());
            return "";
        } else if (eventDescription.length() > maxDescriptionLength) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Описание мероприятия не может быть больше %s символов(""",
                            maxDescriptionLength))
                    .build());
            return "";
        }

        return eventDescription;
    }

    public LocalDateTime validateEventStartTime(Long chatId, String userMessage) {
        try {
            LocalDateTime eventStartTime = LocalDateTime.parse(userMessage, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            if (eventStartTime.isBefore(LocalDateTime.now())) {
                telegramSender.sendText(chatId, SendMessage.builder()
                                .chatId(chatId)
                                .text("""
                                        Эта дата уже прошла. Укажите другое время начала мероприятия.""")
                        .build());
                return null;
            }
            return eventStartTime;
        } catch (DateTimeParseException e) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Некорректный формат даты. Пожалуйста, введите дату в формате:
                                    `DD.MM.YYYY HH:mm`""")
                    .build());

            return null;
        }
    }

    public String validateEventLocation(Long chatId, String userMessage) {
        int maxLocationLength = 70;

        if (userMessage.isBlank()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Адрес мероприятия не может быть пустым(""")
                    .build());
            return null;
        } else if (userMessage.length() > maxLocationLength) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Адрес мероприятия не может быть больше %s символов(""",
                            maxLocationLength))
                    .build());
            return null;
        }

        return userMessage;
    }
}
