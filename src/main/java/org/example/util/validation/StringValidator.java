package org.example.util.validation;

import lombok.RequiredArgsConstructor;
import org.example.util.telegram.api.TelegramSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StringValidator {

    private final TelegramSender telegramSender;

    @Value("${evironment.max-name-length}")
    private int maxNameLength;
    @Value("${evironment.max-description-length}")
    private int maxDescriptionLength;
    @Value("${evironment.max-location-length}")
    private int maxLocationLength;

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

    public String validateAndFormatMiddleName(Long chatId, String middleName) {
        if (middleName == null || middleName.length() <= 1) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отчество должна содержать более одного символа.""")
                    .build());
        } else if (middleName.matches(".*\\d.*")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отчество не должно содержать цифры.""")
                    .build());
        } else if (middleName.contains(" ")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите только отчество, без пробелов.""")
                    .build());
        } else if (!middleName.matches("[а-яА-Я]+")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отчество должно содержать только русские буквы.""")
                    .build());
        } else if (!Character.isUpperCase(middleName.charAt(0))) {
            middleName = Character.toUpperCase(middleName.charAt(0)) + middleName.substring(1);
            return middleName;
        } else {
            return middleName;
        }

        return null;
    }

    public String validateEventName(Long chatId, String eventName) {
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

    public Long validateVisitorID(Long chatId, String userMessage) {
        if (userMessage == null || !userMessage.matches("\\d{5,10}")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            В ID должно быть от 5 до 10 цифр.""")
                    .build());
            return null;
        }

        try {
            return Long.parseLong(userMessage);
        } catch (NumberFormatException e) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ошибка обработки ID. Попробуйте ещё раз.""")
                    .build());
            return null;
        }
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+7|8)\\d{10}$"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
}
