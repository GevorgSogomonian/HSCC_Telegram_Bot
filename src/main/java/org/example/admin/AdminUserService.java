package org.example.admin;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.admin.commands.AdminAllEvent;
import org.example.admin.commands.AdminDeleteEvent;
import org.example.admin.commands.AdminEditEvent;
import org.example.admin.commands.AdminNewEvent;
import org.example.admin.commands.AdminStart;
import org.example.dto.ChatBotRequest;
import org.example.dto.ChatBotResponse;
import org.example.entity.UserState;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final Map<String, Consumer<Update>> commandHandlers = new HashMap<>();
    private final StateManager stateManager;
    private final TelegramApiQueue telegramApiQueue;
    private final TelegramSender telegramSender;
    private final AdminDeleteEvent adminDeleteEvent;
    private final AdminEditEvent adminEditEvent;
    private final AdminStart adminStart;
    private final AdminNewEvent adminNewEvent;
    private final AdminAllEvent adminAllEvent;
    private final UpdateUtil updateUtil;

    public void onUpdateRecieved(Update update) {
        if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        } else if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            UserState currentState = stateManager.getUserState(chatId);
            processMessage(update, currentState);
        }
    }

    public void processMessage(Update update, UserState state) {
        switch (state) {
            //Start
            case START -> adminStart.handleStartState(update);

            //New event
            case ENTERING_EVENT_NAME -> adminNewEvent.eventNameCheck(update);
            case ENTERING_EVENT_DESCRIPTION -> adminNewEvent.eventDescriptionCheck(update);
            case ENTERING_EVENT_PICTURE -> adminNewEvent.eventPictureCheck(update);
            case ENTERING_EVENT_START_TIME -> adminNewEvent.handleEventStartTime(update);
            case CHOOSING_EVENT_DURATION -> adminNewEvent.handleEventDuration(update);
            case ACCEPTING_SAVE_NEW_EVENT -> adminNewEvent.acceptingSavingNewEvent(update);

            //Edit event
            case ACCEPTING_EDITING_EVENT_NAME -> adminEditEvent.acceptingEditingEventName(update);
            case EDITING_EVENT_NAME -> adminEditEvent.checkEditedEventName(update);
            case ACCEPTING_EDITING_EVENT_DESCRIPTION -> adminEditEvent.acceptingEditingEventDescription(update);
            case EDITING_EVENT_DESCRIPTION -> adminEditEvent.checkEditedEventDescription(update);
            case ACCEPTING_EDITING_EVENT_PICTURE -> adminEditEvent.acceptingEditingEventPicture(update);
            case EDITING_EVENT_PICTURE -> adminEditEvent.checkEditedEventPicture(update);
            case ACCEPTING_EDITING_EVENT_START_TIME -> adminEditEvent.acceptingEditingEventStartTime(update);
            case EDITING_EVENT_START_TIME -> adminEditEvent.checkNewEventStartTime(update);
            case ACCEPTING_EDITING_EVENT_DURATION -> adminEditEvent.acceptingEditingEventDuration(update);
            case EDITING_EVENT_DURATION -> adminEditEvent.checkEventDuration(update);
            case ACCEPTING_SAVE_EDITED_EVENT -> adminEditEvent.acceptingSavingEditedEvent(update);

            //Command choosing
            case COMMAND_CHOOSING -> processTextMessage(update);
        }
    }

    @PostConstruct
    public void init() {
        commandHandlers.put("Все мероприятия", adminAllEvent::handleAllEventsCommand);
        commandHandlers.put("Новое мероприятие", adminNewEvent::handleNewEventCommand);
    }

    private void processTextMessage(Update update) {
        String userMessage = update.getMessage().getText();
        commandHandlers.getOrDefault(userMessage, adminStart::handleStartState).accept(update);
    }

    private void processCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[0]) {
            case "delete" -> adminDeleteEvent.processCallbackQuery(update);
            case "edit" -> adminEditEvent.processCallbackQuery(update);
            case "new" -> adminNewEvent.processCallbackQuery(update);
            default -> sendUnknownCallbackResponse(chatId);
        }

//        AnswerCallbackQuery answer = new AnswerCallbackQuery();
//        answer.setCallbackQueryId(callbackQuery.getId());
//        answer.setText("Команда обработана.");
//        answer.setShowAlert(true);
//
//        telegramApiQueue.addRequest(new ChatBotRequest(callbackQuery.getFrom().getId(), answer));
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }
}
