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

            //Edit event
            case EDITING_EVENT_NAME -> adminEditEvent.checkEditedEventName(update);
            case EDITING_EVENT_DESCRIPTION -> adminEditEvent.checkEditedEventDescription(update);
            case EDITING_EVENT_PICTURE -> adminEditEvent.checkEditedEventPicture(update);

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
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);

        if (callbackData.startsWith("delete_event_")) {
            System.out.println("сработало удаление");
            Long eventId = Long.parseLong(callbackData.split("_")[2]);
            adminDeleteEvent.handleDeleteEvent(chatId, eventId);
        } else if (callbackData.startsWith("edit_event_")) {
            Long eventId = Long.parseLong(callbackData.split("_")[2]);
            adminEditEvent.handleEditEvent(chatId, eventId);
        } else if (callbackData.startsWith("duration_")) {
            adminNewEvent.handleEventDuration(update);
        } else {
            sendUnknownCallbackResponse(chatId);
        }

        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        answer.setText("Команда обработана.");
        answer.setShowAlert(true);

        telegramApiQueue.addRequest(new ChatBotRequest(callbackQuery.getFrom().getId(), answer));
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    private String getMimeTypeByExtension(String filePath) {
        Map<String, String> mimeTypes = Map.of(
                "jpg", "image/jpeg",
                "png", "image/png",
                "gif", "image/gif",
                "pdf", "application/pdf",
                "txt", "text/plain"
        );

        String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
        return mimeTypes.get(extension);
    }
}