package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.EventDestructor;
import org.example.data_classes.dto.ChatBotRequest;
import org.example.data_classes.data_base.entity.EventNotification;
import org.example.data_classes.enums.UserState;
import org.example.data_classes.data_base.entity.Event;
import org.example.repository.EventDestructorRepository;
import org.example.util.schedulers.notifications.EventNotificationService;
import org.example.repository.EventRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramApiQueue;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.ActionsChainUtil;
import org.example.util.validation.StringValidator;
import org.example.util.state.TemporaryDataService;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminNewEvent {

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final EventRepository eventRepository;
    private final TelegramApiQueue telegramApiQueue;
    private final AdminStart adminStart;
    private final StringValidator stringValidator;
    private final TemporaryDataService<Event> temporaryEventService;
    private final ActionsChainUtil actionsChainUtil;
    private final EventNotificationService eventNotificationService;
    private final EventDestructorRepository eventDestructorRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "accept-creating-new-event" -> acceptCreatingNewEvent(chatId, messageId);
            case "cancel-creating-new-event" -> cancelEditingEvent(chatId, messageId);
            case "duration" -> handleEventDuration(update);
            default -> sendUnknownCallbackResponse(chatId);
        }

        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    public void handleNewEventCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                .text("Да")
                .callbackData("new_accept-creating-new-event")
                .build();

        InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                .text("Нет")
                .callbackData("new_cancel-creating-new-event")
                .build();

        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboardRow(List.of(yesButton, noButton))
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Начать создание нового мероприятия?""")
                .replyMarkup(inlineKeyboardMarkup)
                .build());
    }

    private void acceptCreatingNewEvent(Long chatId, Integer messageId) {
        Event newEvent = new Event();
        newEvent.setCreatorChatId(chatId);
        temporaryEventService.putTemporaryData(chatId, newEvent);
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                            Создаём новое мероприятие.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());

        requestEventName(chatId);
    }

    private void cancelEditingEvent(Long chatId, Integer messageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Создание нового мероприятия отменено.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    private void requestEventName(Long chatId) {
        ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                .removeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите *название* мероприятия:""")
                .replyMarkup(replyKeyboardRemove)
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_NAME);
    }

    public void eventNameCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String eventName = update.getMessage().getText();
        String validatedEventName = stringValidator.validateEventName(chatId, eventName);

        if (!validatedEventName.isEmpty()) {
            if (eventRepository.findByEventName(eventName).isPresent()) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                        Мероприятие с названием: %s уже существует.
                                        
                                        В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""",
                                eventName))
                        .build());
            } else {
                Event newEvent = temporaryEventService.getTemporaryData(chatId);
                newEvent.setEventName(validatedEventName);
                temporaryEventService.putTemporaryData(chatId, newEvent);

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                    Отлично! Название сохранено!""")
                        .build());

                requestEventDescription(chatId);
            }
        }
    }

    private void requestEventDescription(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите *описание* мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_DESCRIPTION);
    }

    public void eventDescriptionCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String eventDescription = update.getMessage().getText();
        String validatedEventDescription = stringValidator.validateEventDescription(chatId, eventDescription);

        if (!validatedEventDescription.isEmpty()) {
            Event temporaryEvent = temporaryEventService.getTemporaryData(chatId);
            temporaryEvent.setDescription(validatedEventDescription);
            temporaryEventService.putTemporaryData(chatId, temporaryEvent);

            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                    Отлично! Описание мероприятия сохранено!""")
                    .build());

            requestEventPicture(chatId);
        }
    }

    private void requestEventPicture(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Пришлите *обложку* мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_PICTURE);
    }

    public void eventPictureCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.getMessage().hasDocument()) {
            String fileId = updateUtil.getFileId(update);
            try {
                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("Изображение сохранено.")
                        .build());

                requestEventStartTime(chatId);
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
        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                Введите *дату и время начала* мероприятия в формате:
                
                `DD.MM.YYYY HH:mm`""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_START_TIME);
    }

    public void handleEventStartTime(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        LocalDateTime validatedEventStartTime = stringValidator.validateEventStartTime(chatId, userMessage);

        if (validatedEventStartTime != null) {
            Event event = temporaryEventService.getTemporaryData(chatId);
            event.setStartTime(validatedEventStartTime);
            temporaryEventService.putTemporaryData(chatId, event);

            requestEventDuration(chatId);
        }
    }

    private void requestEventDuration(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                createDurationButton("1 час", "new_duration_1h"),
                createDurationButton("1.5 часа", "new_duration_1.5h")
        ));
        rows.add(List.of(
                createDurationButton("2 часа", "new_duration_2h"),
                createDurationButton("3 часа", "new_duration_3h")
        ));
        markup.setKeyboard(rows);

        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Выберите *продолжительность* мероприятия:""")
                        .replyMarkup(markup)
                .build());

        stateManager.setUserState(chatId, UserState.CHOOSING_EVENT_DURATION);
    }

    public void handleEventDuration(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();
            Long chatId = callbackQuery.getMessage().getChatId();

            Long durationInMinutes = switch (callbackData) {
                case "new_duration_1h" -> 60L;
                case "new_duration_1.5h" -> 90L;
                case "new_duration_2h" -> 120L;
                case "new_duration_3h" -> 180L;
                default -> null;
            };

            if (durationInMinutes != null) {
                Event event = temporaryEventService.getTemporaryData(chatId);
                event.setDuration(Duration.ofMinutes(durationInMinutes));
                temporaryEventService.putTemporaryData(chatId, event);

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                            Продолжительность мероприятия сохранена.""")
                        .build());

                telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(callbackQuery.getMessage().getMessageId())
                        .build());

                telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQuery.getId())
                        .text("""
                                Команда обработана.""")
                        .showAlert(false)
                        .build());

                requestEventLocation(chatId);
            } else {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                    Некорректный выбор. Попробуйте снова.""")
                        .build());
            }
        }
    }

    private void requestEventLocation(Long chatId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                Укажите *место проведения* мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.ENTERING_EVENT_LOCATION);
    }

    public void handleEventLocation(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        String eventLocation = stringValidator.validateEventLocation(chatId, userMessage);

        if (eventLocation != null) {
            Event event = temporaryEventService.getTemporaryData(chatId);
            event.setEventLocation(eventLocation);
            temporaryEventService.putTemporaryData(chatId, event);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Место проведения мероприятия сохранено.""")
                    .build());

            offerSaveNewEvent(chatId);
        }
    }

    private void offerSaveNewEvent(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                *Сохранить* новое мероприятие?""", UserState.ACCEPTING_SAVE_NEW_EVENT);
    }

    public void acceptingSavingNewEvent(Update update) {
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            acceptSavingNewEvent(update);
        } else {
            cancelSavingNewEvent(update);
        }
    }

    private void acceptSavingNewEvent(Update update) {
        Long chatId = updateUtil.getChatId(update);

        Thread pictureSaveThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Long eventId = eventRepository.save(temporaryEventService.getTemporaryData(chatId)).getId();
            saveNotification(chatId, eventId);
            saveDestructionMessage(chatId, eventId);
            System.out.println("""
                    Сработал сохранение)""");
        });
        pictureSaveThread.start();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                            Новое мероприятие сохранено.""")
                .build());

        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        adminStart.handleStartState(update);
    }

    private void saveNotification(Long chatId, Long eventId) {
        Event event = temporaryEventService.getTemporaryData(chatId);
        EventNotification eventNotification = new EventNotification();
        eventNotification.setNotificationText(String.format("""
                    Напоминаем, что *%s* состоится мероприятие *%s*!""",
                event.getFormattedStartDate(), event.getEventName()));
        eventNotification.setNotificationTime(event.getStartTime().minusHours(24));
        eventNotification.setEventId(eventId);

        eventNotificationService.saveNotification(eventNotification);
    }

    private void saveDestructionMessage(Long chatId, Long eventId) {
        Event event = temporaryEventService.getTemporaryData(chatId);
        EventDestructor eventDestructor = new EventDestructor();
        eventDestructor.setDestructionTime(event.getStartTime().plusHours(24));
        eventDestructor.setEventId(eventId);

        eventDestructorRepository.save(eventDestructor);
    }

    private void cancelSavingNewEvent(Update update) {
        Long chatId = updateUtil.getChatId(update);
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Новое мероприятие не сохранено.""")
                .build());
        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        adminStart.handleStartState(update);
    }

    private InlineKeyboardButton createDurationButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
}
