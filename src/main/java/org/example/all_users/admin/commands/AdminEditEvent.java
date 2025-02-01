package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.dto.ChatBotRequest;
import org.example.data_classes.enums.UserState;
import org.example.data_classes.data_base.entity.Event;
import org.example.repository.EventDestructorRepository;
import org.example.repository.EventNotificationRepository;
import org.example.repository.EventRepository;
import org.example.util.telegram.helpers.ActionsChainUtil;
import org.example.util.telegram.helpers.CallbackUtil;
import org.example.util.validation.StringValidator;
import org.example.util.state.TemporaryDataService;
import org.example.util.image.ImageService;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramApiQueue;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminEditEvent {

    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final UpdateUtil updateUtil;
    private final ImageService imageService;
    private final TelegramApiQueue telegramApiQueue;
    private final AdminStart adminStart;

    private final TemporaryDataService<Event> temporaryEditedEventService;
    private final StringValidator stringValidator;
    private final CallbackUtil callbackUtil;
    private final ActionsChainUtil actionsChainUtil;
    private final EventNotificationRepository eventNotificationRepository;
    private final EventDestructorRepository eventDestructorRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "offer-editing-event" -> handleEditEvent(chatId, callbackData, messageId);
            case "accept-editing-event" -> acceptEventEditing(chatId, callbackData, messageId);
            case "cancel-editing-event" -> cancelEditingEvent(chatId, messageId);
            case "duration" -> checkEventDuration(update);
            default -> callbackUtil.sendUnknownCallbackResponse(chatId);
        }

        callbackUtil.answerCallback(chatId, callbackQuery.getId());
    }

    public void handleEditEvent(Long chatId, String callbackText, Integer oldMessageId) {
        Long eventId = Long.parseLong(callbackText.split("_")[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            String eventName = eventOptional.get().getEventName();
            InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                    .text("Да")
                    .callbackData(String.format("edit_accept-editing-event_%s_old-message-id_%s", eventId, oldMessageId))
                    .build();

            InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                    .text("Нет")
                    .callbackData("edit_cancel-editing-event")
                    .build();

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(yesButton, noButton))
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                                Начать редактирование мероприятия: *%s* ?""", eventName))
                    .replyMarkup(inlineKeyboardMarkup)
                    .build());
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Мероприятие не найдено!""")
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                            .chatId(chatId)
                            .messageId(oldMessageId)
                    .build());
        }
    }

    private void acceptEventEditing(Long chatId, String callbackText, Integer messageId) {
        Long eventId = Long.parseLong(callbackText.split("_")[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event editedEvent = eventOptional.get();
            temporaryEditedEventService.putTemporaryData(chatId, editedEvent);
            ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                                    Редактируем мероприятие: *%s*""", editedEvent.getEventName()))
                    .replyMarkup(replyKeyboardRemove)
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());

            offerEnterNewName(chatId);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятие не найдено!""")
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
        }
    }

    private void cancelEditingEvent(Long chatId, Integer messageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Редактирование отменено.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                .build());
    }

    private void offerEnterNewName(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *название* мероприятия?""", UserState.ACCEPTING_EDITING_EVENT_NAME);
    }

    public void acceptingEditingEventName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEventName(chatId, editedEvent);
        } else {
            offerEnterNewDescription(chatId);
        }
    }

    private void requestNewEventName(Long chatId, Event event) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущее название:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", event.getEventName()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новое название для мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_NAME);
    }

    public void checkEditedEventName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Optional<Event> eventOptional = eventRepository.findByEventName(editedEvent.getEventName());
        String validatedEventName = stringValidator.validateEventName(chatId, update.getMessage().getText());

        if (!validatedEventName.isEmpty()) {
            if (eventOptional.isPresent() && !eventOptional.get().getId().equals(editedEvent.getId())) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                Мероприятие с названием: *%s* уже существует.
                                
                                В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""", validatedEventName))
                        .build());
            } else {
                editedEvent.setEventName(validatedEventName);
                temporaryEditedEventService.putTemporaryData(chatId, editedEvent);

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Отлично! Название сохранено!""")
                        .build());

                offerEnterNewDescription(chatId);
            }
        }
    }

    private void offerEnterNewDescription(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *описание* мероприятия?""", UserState.ACCEPTING_EDITING_EVENT_DESCRIPTION);
    }

    public void acceptingEditingEventDescription(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEventDescription(chatId, editedEvent);
        } else {
            offerEnterNewPicture(chatId);
        }
    }

    private void requestNewEventDescription(Long chatId, Event editedEvent) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущее описание:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                                %s""", editedEvent.getDescription()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новое описание мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_DESCRIPTION);
    }

    public void checkEditedEventDescription(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String eventDescription = update.getMessage().getText();
        String validatedEventDescription = stringValidator.validateEventDescription(chatId, eventDescription);

        if (!validatedEventDescription.isEmpty()) {
            Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
            editedEvent.setDescription(eventDescription);
            temporaryEditedEventService.putTemporaryData(chatId, editedEvent);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новое описание мероприятия сохранено!""")
                    .build());

            offerEnterNewPicture(chatId);
        }
    }

    private void offerEnterNewPicture(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *обложку* мероприятия?""", UserState.ACCEPTING_EDITING_EVENT_PICTURE);
    }

    public void acceptingEditingEventPicture(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEventPicture(chatId, editedEvent);
        } else {
            offerNewStartTime(chatId);
        }
    }

    private void requestNewEventPicture(Long chatId, Event editedEvent) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущая обложка:""")
                .build());

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);

        telegramSender.sendPhoto(chatId, editedEvent.getId(), sendPhoto);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Пришлите новую обложку мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_PICTURE);
    }

    public void checkEditedEventPicture(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.getMessage().hasDocument()) {
            String fileId = updateUtil.getFileId(update);
            try {
                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Новое изображение сохранено.""")
                        .build());

                offerNewStartTime(chatId);
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

    private void offerNewStartTime(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *время начала* мероприятия?""", UserState.ACCEPTING_EDITING_EVENT_START_TIME);
    }

    public void acceptingEditingEventStartTime(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEventStartTime(chatId, editedEvent);
        } else {
            offerNewDuration(chatId);
        }
    }

    private void requestNewEventStartTime(Long chatId, Event editedEvent) {
        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                Текущее время начала мероприятия:
                                
                                *%s*""", editedEvent.getFormattedStartDate()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                Введите новые дату и время начала мероприятия в формате:
                `DD.MM.YYYY HH:mm`""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_START_TIME);
    }

    public void checkNewEventStartTime(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        LocalDateTime validatedEventStartTime = stringValidator.validateEventStartTime(chatId, userMessage);

        if (validatedEventStartTime != null) {
            Event event = temporaryEditedEventService.getTemporaryData(chatId);
            event.setStartTime(validatedEventStartTime);
            temporaryEditedEventService.putTemporaryData(chatId, event);

            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Новые дата и время начала мероприятия сохранены.""")
                    .build());

            offerNewDuration(chatId);
        }
    }

    private void offerNewDuration(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *продолжительность* мероприятия?""", UserState.ACCEPTING_EDITING_EVENT_DURATION);
    }

    public void acceptingEditingEventDuration(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEventDuration(chatId, editedEvent);
        } else {
            offerNewEventLocation(chatId);
        }
    }

    private void requestNewEventDuration(Long chatId, Event editedEvent) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                Текущая продолжительность мероприятия:
                                
                                *%s*""", editedEvent.getFormattedDuration()))
                .build());

        rows.add(List.of(
                createDurationButton("1 час", "edit_duration_1h"),
                createDurationButton("1.5 часа", "edit_duration_1.5h")
        ));
        rows.add(List.of(
                createDurationButton("2 часа", "edit_duration_2h"),
                createDurationButton("3 часа", "edit_duration_3h")
        ));
        markup.setKeyboard(rows);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Выберите новую продолжительность мероприятия:""")
                .replyMarkup(markup)
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_DURATION);
    }

    public void checkEventDuration(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();
            Long chatId = callbackQuery.getMessage().getChatId();

            Long durationInMinutes = switch (callbackData) {
                case "edit_duration_1h" -> 60L;
                case "edit_duration_1.5h" -> 90L;
                case "edit_duration_2h" -> 120L;
                case "edit_duration_3h" -> 180L;
                default -> null;
            };

            if (durationInMinutes != null) {
                Event event = temporaryEditedEventService.getTemporaryData(chatId);
                event.setDuration(Duration.ofMinutes(durationInMinutes));
                temporaryEditedEventService.putTemporaryData(chatId, event);

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                            Новая продолжительность мероприятия сохранена!""")
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

                offerNewEventLocation(chatId);
            } else {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                    Некорректный выбор. Попробуйте снова.""")
                        .build());
            }
        }
    }

    private void offerNewEventLocation(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                Хотите изменить *место проведения* мероприятия?""", UserState.ACCEPTING_EDITING_EVENT_LOCATION);
    }

    public void acceptingEditingEventLocation(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            requestNewEventLocation(chatId, editedEvent);
        } else {
            offerSaveEditedEvent(chatId);
        }
    }

    private void requestNewEventLocation(Long chatId, Event editedEvent) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущее место проведения мероприятия:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                *%s*""", editedEvent.getEventLocation()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Введите новый адрес:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_LOCATION);
    }

    public void checkNewEventLocation(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        String eventLocation = stringValidator.validateEventLocation(chatId, userMessage);

        if (eventLocation != null) {
            Event event = temporaryEditedEventService.getTemporaryData(chatId);
            event.setEventLocation(eventLocation);
            temporaryEditedEventService.putTemporaryData(chatId, event);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Новое место проведения мероприятия сохранено.""")
                    .build());

            offerSaveEditedEvent(chatId);
        }
    }

    private void offerSaveEditedEvent(Long chatId) {
        actionsChainUtil.offerNextAction(chatId, """
                *Сохранить* отредактированное мероприятие?""", UserState.ACCEPTING_SAVE_EDITED_EVENT);
    }

    public void acceptingSavingEditedEvent(Update update) {
        Boolean answer = actionsChainUtil.checkAnswer(update);

        if (answer == null) {
            return;
        }

        if (answer) {
            acceptSavingEditedEvent(update);
        } else {
            cancelSavingEditedEvent(update);
        }
    }

    private void acceptSavingEditedEvent(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Event oldEvent = eventRepository.findById(editedEvent.getId()).get();
        String oldImageUrl = oldEvent.getImageUrl();

        if (oldImageUrl != null && !oldImageUrl.isBlank() && !editedEvent.getImageUrl().equals(oldImageUrl)) {
            String bucketName = "pictures";
            imageService.deleteImage(bucketName, oldImageUrl);
            System.out.println("""
                    Сработал удаление)""");
        }

        Thread pictureSaveThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Long eventId = eventRepository.save(temporaryEditedEventService.getTemporaryData(chatId)).getId();
            editNotificationTime(chatId, eventId);
            editDestructionTime(chatId, eventId);
            System.out.println("""
                    Сработал сохранение)""");
        });
        pictureSaveThread.start();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Отредактированное мероприятие сохранено.""")
                .build());
        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        adminStart.handleStartState(update);
    }

    private void editNotificationTime(Long chatId, Long eventId) {
        Event event = temporaryEditedEventService.getTemporaryData(chatId);

        eventNotificationRepository.updateNotification(eventId, event.getStartTime().minusHours(24), String.format("""
                    Напоминаем, что *%s* состоится мероприятие *%s*!""",
                event.getFormattedStartDate(), event.getEventName()));
    }

    private void editDestructionTime(Long chatId, Long eventId) {
        Event event = temporaryEditedEventService.getTemporaryData(chatId);

        eventDestructorRepository.updateTime(eventId, event.getStartTime().plusHours(24));
    }

    private void cancelSavingEditedEvent(Update update) {
        Long chatId = updateUtil.getChatId(update);
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Отредактированное мероприятие не сохранено.""")
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
