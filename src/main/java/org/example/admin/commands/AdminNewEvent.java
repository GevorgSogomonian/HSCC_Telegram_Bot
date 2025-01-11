package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotRequest;
import org.example.entity.UserState;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminNewEvent {

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final EventRepository eventRepository;
    private final TelegramApiQueue telegramApiQueue;
    private final AdminStart adminStart;

    public void handleNewEventCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                Введите название мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_NAME);
    }

    public void eventNameCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int maxNameLength = 120;

        if (eventRepository.findByEventName(userMessage).isPresent()) {
            sendMessage.setText(String.format("""
                    Мероприятие с названием: %s уже существует.
                    
                    В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""",
                    userMessage));
            telegramSender.sendText(chatId, sendMessage);
        } else if (userMessage.isBlank()) {
            sendMessage.setText("""
                    Название мероприятия не может быть пустым(""");
            telegramSender.sendText(chatId, sendMessage);
        } else if (userMessage.length() > maxNameLength) {
            sendMessage.setText(String.format("""
                    Название мероприятия не может быть больше %s символов(""",
                    maxNameLength));
            telegramSender.sendText(chatId, sendMessage);
        } else {
            Event newEvent = new Event();
            newEvent.setCreatorChatId(chatId);
            newEvent.setEventName(userMessage);

            eventRepository.save(newEvent);
            sendMessage.setText("""
                    Отлично! Название сохранено!""");
            telegramSender.sendText(chatId, sendMessage);
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Введите описание мероприятия:""")
                    .build());
            stateManager.setUserState(chatId, UserState.ENTERING_EVENT_DESCRIPTION);
        }
    }

    public void eventDescriptionCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int maxDescriptioonLength = 2000;

        Optional<Event> eventOptional = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId);

        if (userMessage.isBlank()) {
            sendMessage.setText("""
                    Описание мероприятия не может быть пустым(""");
            telegramSender.sendText(chatId, sendMessage);
        } else if (userMessage.length() > maxDescriptioonLength) {
            sendMessage.setText(String.format("""
                    Описание мероприятия не может быть больше %s символов(""",
                    maxDescriptioonLength));
            telegramSender.sendText(chatId, sendMessage);
        } else {
            Event event = eventOptional.get();
            event.setDescription(userMessage);
            eventRepository.save(event);
            sendMessage.setText("""
                    Отлично! Описание мероприятия сохранено!""");
            telegramSender.sendText(chatId, sendMessage);
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Пришлите обложку мероприятия:""")
                    .build());
            stateManager.setUserState(chatId, UserState.ENTERING_EVENT_PICTURE);
        }
    }

    public void eventPictureCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.getMessage().hasDocument()) {
            String fileId = updateUtil.getFileId(update);
            try {
                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("Изображение успешно сохранено.")
                        .build());

                requestEventStartTime(chatId);
//                stateManager.setUserState(chatId, UserState.ENTERING_EVENT_START_TIME);
//                adminStart.handleStartState(update);
            } catch (Exception e) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("Ошибка при добавлении запроса в очередь: " + e.getMessage())
                        .build());
            }
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("Пожалуйста, отправьте изображение для обложки мероприятия файлом.")
                    .build());
        }
    }

    private void requestEventStartTime(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("""
                Введите дату и время начала мероприятия в формате:
                `YYYY-MM-DD HH:mm`""");
//        message.setParseMode("Markdown");

        telegramSender.sendText(chatId, message);

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_START_TIME);
    }

    public void handleEventStartTime(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();

        try {
            LocalDateTime startTime = LocalDateTime.parse(userMessage, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            Optional<Event> eventOptional = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId);

            if (eventOptional.isPresent()) {
                Event event = eventOptional.get();
                event.setStartTime(startTime);
                eventRepository.save(event);

                requestEventDuration(chatId);
            } else {
                telegramSender.sendText(chatId, SendMessage.builder()
                                .chatId(chatId)
                                .text("""
                                        Мероприятие не найдено. Попробуйте снова.""")
                        .build());
            }
        } catch (DateTimeParseException e) {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Некорректный формат даты. Пожалуйста, введите дату в формате:
                                    `YYYY-MM-DD HH:mm`""")
                    .build());
        }
    }

    private void requestEventDuration(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите продолжительность мероприятия:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                createDurationButton("1 час", "duration_1h"),
                createDurationButton("1.5 часа", "duration_1.5h")
        ));
        rows.add(List.of(
                createDurationButton("2 часа", "duration_2h"),
                createDurationButton("3 часа", "duration_3h")
        ));

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        telegramSender.sendText(chatId, message);

        stateManager.setUserState(chatId, UserState.CHOOSING_EVENT_DURATION);
    }

    public void handleEventDuration(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData(); // Здесь значение типа "duration_1h", "duration_1.5h" и т.д.
        Long chatId = callbackQuery.getMessage().getChatId();

        // Сразу возвращаем продолжительность в минутах
        Long durationInMinutes = switch (callbackData) {
            case "duration_1h" -> 60L;
            case "duration_1.5h" -> 90L;
            case "duration_2h" -> 120L;
            case "duration_3h" -> 180L;
            default -> null;
        };

        if (durationInMinutes != null) {
            Optional<Event> eventOptional = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId);

            if (eventOptional.isPresent()) {
                Event event = eventOptional.get();
                event.setDuration(Duration.ofMinutes(durationInMinutes)); // Устанавливаем продолжительность
                eventRepository.save(event);

                telegramSender.sendText(chatId, SendMessage.builder()
                                .chatId(chatId)
                                .text("""
                                        Мероприятие успешно создано!""")
                        .build());
            } else {
                telegramSender.sendText(chatId, SendMessage.builder()
                                .chatId(chatId)
                                .text("""
                                        Мероприятие не найдено. Попробуйте снова.""")
                        .build());
            }
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Некорректный выбор. Попробуйте снова.""")
                    .build());
        }

        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        adminStart.handleStartState(update);
    }

    private InlineKeyboardButton createDurationButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData); // Передаём точное значение (например, "1", "1.5", "2", "3")
        return button;
    }
}
