package org.example.base_user;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.base_user.commands.BaseActualEvents;
import org.example.base_user.commands.BaseStart;
import org.example.entity.UserState;
import org.example.state_manager.StateManager;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
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

    public void onUpdateRecieved(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
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
        }
    }

    @PostConstruct
    public void init() {
        commandHandlers.put("Актуальные мероприятия", baseActualEvents::handleActualEventsCommand);
    }

    private void processTextMessage(Update update) {
        String userMessage = update.getMessage().getText();
        System.out.printf("""
                userMessage: %s%n""", userMessage);
        commandHandlers.getOrDefault(userMessage, baseStart::handleStartState).accept(update);
    }
}
