package org.example.util;

import lombok.RequiredArgsConstructor;
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

    public String validateAndFormatFirstName(Long chatId, String firstName) {
        if (firstName == null || firstName.length() <= 1) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Имя должно содержать более одного символа.""")
                    .build());
        } else if (firstName.matches(".*\\d.*")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Имя не должно содержать цифры.""")
                    .build());
        } else if (firstName.contains(" ")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Введите только имя, без пробелов.""")
                    .build());
        } else if (!firstName.matches("[а-яА-Я]+")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Имя должно содержать только русские буквы.""")
                    .build());
        } else if (!Character.isUpperCase(firstName.charAt(0))) {
            firstName = Character.toUpperCase(firstName.charAt(0)) + firstName.substring(1);
            return firstName;
        } else {
            return firstName;
        }

        return null;
    }

    public String validateAndFormatLastName(Long chatId, String lastName) {
        if (lastName == null || lastName.length() <= 1) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Фамилия должна содержать более одного символа.""")
                    .build());
        } else if (lastName.matches(".*\\d.*")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Фамилия не должна содержать цифры.""")
                    .build());
        } else if (lastName.contains(" ")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите только фамилию, без пробелов.""")
                    .build());
        } else if (!lastName.matches("[а-яА-Я]+")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Фамилия должна содержать только русские буквы.""")
                    .build());
        } else if (!Character.isUpperCase(lastName.charAt(0))) {
            lastName = Character.toUpperCase(lastName.charAt(0)) + lastName.substring(1);
            return lastName;
        } else {
            return lastName;
        }

        return null;
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
