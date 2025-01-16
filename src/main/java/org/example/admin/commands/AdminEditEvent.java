package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotRequest;
import org.example.entity.UserState;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.util.StringValidator;
import org.example.util.TemporaryDataService;
import org.example.image.ImageService;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.InputStream;
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

    public void handleEditEvent(Long chatId, String callbackText, Integer oldMessageId) {
        Long eventId = Long.parseLong(callbackText.split("_")[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event editedEvent = eventOptional.get();
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
//            temporaryEditedEventService.putTemporaryData(chatId, editedEvent);
//            telegramSender.sendText(chatId, SendMessage.builder()
//                            .chatId(chatId)
//                            .text(String.format("""
//                                    Редактируем мероприятие: *%s*""", editedEvent.getEventName()))
//                    .build());
//
//            offerEnterNewName(chatId);
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
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                                    Редактируем мероприятие: *%s*""", editedEvent.getEventName()))
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
        KeyboardButton yesButton = new KeyboardButton("Да");
        KeyboardButton noButton = new KeyboardButton("Нет");

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of(new KeyboardRow(List.of(yesButton, noButton))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Хотите изменить название мероприятия?""")
                        .replyMarkup(keyboardMarkup)
                .build());

        stateManager.setUserState(chatId, UserState.ACCEPTING_EDITING_EVENT_NAME);
    }

    public void acceptingEditingEventName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText().toLowerCase();
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);

        if (userMessage.equals("да")) {
            requestNewEventName(chatId, editedEvent);
        } else if (userMessage.equals("нет")) {
            offerEnterNewDescription(chatId);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Введите 'да' или 'нет'""")
                    .build());
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
        String eventName = update.getMessage().getText();
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Long eventId = editedEvent.getId();
        Optional<Event> eventOptional = eventRepository.findByEventName(editedEvent.getEventName());
        String validatedEventName = stringValidator.validateEventName(chatId, eventName);

        if (!validatedEventName.isEmpty()) {
            if (eventOptional.isPresent() && !eventOptional.get().getId().equals(eventId)) {
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
        KeyboardButton yesButton = new KeyboardButton("Да");
        KeyboardButton noButton = new KeyboardButton("Нет");

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of(new KeyboardRow(List.of(yesButton, noButton))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Хотите изменить описание мероприятия?""")
                .replyMarkup(keyboardMarkup)
                .build());

        stateManager.setUserState(chatId, UserState.ACCEPTING_EDITING_EVENT_DESCRIPTION);
    }

    public void acceptingEditingEventDescription(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText().toLowerCase();
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);

        if (userMessage.equals("да")) {
            requestNewEventDescription(chatId, editedEvent);
        } else if (userMessage.equals("нет")) {
            offerEnterNewPicture(chatId);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Введите 'да' или 'нет'""")
                    .build());
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
        KeyboardButton yesButton = new KeyboardButton("Да");
        KeyboardButton noButton = new KeyboardButton("Нет");

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of(new KeyboardRow(List.of(yesButton, noButton))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Хотите изменить обложку мероприятия?""")
                .replyMarkup(keyboardMarkup)
                .build());

        stateManager.setUserState(chatId, UserState.ACCEPTING_EDITING_EVENT_PICTURE);
    }

    public void acceptingEditingEventPicture(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText().toLowerCase();
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);

        if (userMessage.equals("да")) {
            requestNewEventPicture(chatId, editedEvent);
        } else if (userMessage.equals("нет")) {
            offerSaveEditedEvent(chatId);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите 'да' или 'нет'""")
                    .build());
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

                offerSaveEditedEvent(chatId);
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

    private void offerSaveEditedEvent(Long chatId) {
        KeyboardButton yesButton = new KeyboardButton("Да");
        KeyboardButton noButton = new KeyboardButton("Нет");

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboard(List.of(new KeyboardRow(List.of(yesButton, noButton))))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Сохранить отредактированное мероприятия?""")
                .replyMarkup(keyboardMarkup)
                .build());

        stateManager.setUserState(chatId, UserState.ACCEPTING_SAVE_EDITED_EVENT);
    }

    public void acceptingSavingEditedEvent(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText().toLowerCase();

        if (userMessage.equals("да")) {
            saveEditedEvent(update);
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Отредактированное мероприятие сохранено.""")
                    .build());
            stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
            adminStart.handleStartState(update);
        } else if (userMessage.equals("нет")) {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Отредактированное мероприятие не сохранено.""")
                    .build());
            stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
            adminStart.handleStartState(update);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите 'да' или 'нет'""")
                    .build());
        }
    }

    private void saveEditedEvent(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Event oldEvent = eventRepository.findById(editedEvent.getId()).get();
        String oldImageUrl = oldEvent.getImageUrl();

        if (oldImageUrl != null && !oldImageUrl.isBlank()) {
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
            eventRepository.save(temporaryEditedEventService.getTemporaryData(chatId));
            System.out.println("""
                    Сработал сохранение)""");
        });
        pictureSaveThread.start();
    }
}
