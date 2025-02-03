package org.example.all_users.base_user;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.all_users.base_user.commands.BaseActualEvents;
import org.example.all_users.base_user.commands.BaseEditInfo;
import org.example.all_users.base_user.commands.BaseMyEvents;
import org.example.all_users.base_user.commands.BaseStart;
import org.example.all_users.base_user.commands.BaseSubscribeToEvent;
import org.example.all_users.base_user.commands.BaseUnsubscribeFromEvent;
import org.example.all_users.base_user.commands.BaseUserInfo;
import org.example.all_users.base_user.commands.ReturnToAdminMode;
import org.example.data_classes.enums.UserState;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class BaseUserService {

    private final BaseStart baseStart;
    private final BaseActualEvents baseActualEvents;

    private final Map<String, Consumer<Update>> commandHandlers = new HashMap<>();
    private final StateManager stateManager;
    private final UpdateUtil updateUtil;
    private final BaseUserInfo baseUserInfo;
    private final BaseSubscribeToEvent baseSubscribeToEvent;
    private final TelegramSender telegramSender;
    private final BaseUnsubscribeFromEvent baseUnsubscribeFromEvent;
    private final BaseMyEvents baseMyEvents;
    private final ReturnToAdminMode returnToAdminMode;
    private final BaseEditInfo baseEditInfo;

    public void onUpdateRecieved(Update update) {
        if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            UserState currentState = stateManager.getUserState(chatId);
            processMessage(update, currentState);
        }
    }

    public void processMessage(Update update, UserState state) {
        System.out.printf("""
                userState1: %s%n""", stateManager.getUserState(updateUtil.getChatId(update)));
        switch (state) {
            case START -> baseStart.handleStartState(update);
            case COMMAND_CHOOSING -> processTextMessage(update);

            //edit profile
            case ACCEPTING_EDITING_PROFILE_FIRST_NAME -> baseEditInfo.acceptingEditingFirstName(update);
            case EDITING_PROFILE_FIRST_NAME -> baseEditInfo.checkEditedFirstName(update);
            case ACCEPTING_EDITING_PROFILE_LAST_NAME -> baseEditInfo.acceptingEditingLastName(update);
            case EDITING_PROFILE_LAST_NAME -> baseEditInfo.checkEditedLastName(update);
            case ACCEPTING_EDITING_PROFILE_MIDDLE_NAME -> baseEditInfo.acceptingEditingMiddleName(update);
            case EDITING_PROFILE_MIDDLE_NAME -> baseEditInfo.checkEditedMiddleName(update);
            case ACCEPTING_EDITING_PROFILE_EMAIL -> baseEditInfo.acceptingEditingEmail(update);
            case EDITING_PROFILE_EMAIL -> baseEditInfo.checkEditedEmail(update);
            case ACCEPTING_EDITING_PROFILE_PHONE_NUMBER -> baseEditInfo.acceptingEditingPhoneNumber(update);
            case EDITING_PROFILE_PHONE_NUMBER -> baseEditInfo.checkEditedPhoneNumber(update);
            case ACCEPTING_SAVE_EDITED_PROFILE -> baseEditInfo.acceptingSavingEditedEvent(update);

            default -> baseStart.handleStartState(update);
        }
    }

    private void processCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[0]) {
            case "subscribe" -> baseSubscribeToEvent.processCallbackQuery(update);
            case "unsubscribe" -> baseUnsubscribeFromEvent.processCallbackQuery(update);
            case "user-edit" -> baseEditInfo.processCallbackQuery(update);
            default -> sendUnknownCallbackResponse(chatId);
        }
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    @PostConstruct
    public void init() {
        commandHandlers.put("Актуальные мероприятия", baseActualEvents::handleActualEventsCommand);
        commandHandlers.put("Мои мероприятия", baseMyEvents::handleMyEventsCommand);
        commandHandlers.put("Информация о себе", baseUserInfo::handleUserInfoCommand);
        commandHandlers.put("Вернуться в режим админа", returnToAdminMode::handleReturnToAdminModeCommand);
    }

    private void processTextMessage(Update update) {
        String userMessage = update.getMessage().getText();
        System.out.printf("""
                userMessage: %s%n""", userMessage);
        commandHandlers.getOrDefault(userMessage, baseStart::handleStartState).accept(update);
    }
}
