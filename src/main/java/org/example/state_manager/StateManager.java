package org.example.state_manager;

import org.example.entity.BotState;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {
    private final Map<Long, BotState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, Long> eventEditing = new ConcurrentHashMap<>();

    public BotState getUserState(Long userId) {
        return userStates.getOrDefault(userId, BotState.START);
    }

    public void setUserState(Long userId, BotState state) {
        userStates.put(userId, state);
    }

    public void removeUserState(Long userId) {
        userStates.remove(userId);
    }

    public boolean userStateExists(Long userId) {
        return userStates.containsKey(userId);
    }

    public void setEventBeingEdited(Long chatId, Long eventId) {
        eventEditing.put(chatId, eventId);
    }

    public Long getEventBeingEdited(Long chatId) {
        return eventEditing.get(chatId);
    }

    public void doneEventEditing(Long chatId) {
        eventEditing.remove(chatId);
    }
}
