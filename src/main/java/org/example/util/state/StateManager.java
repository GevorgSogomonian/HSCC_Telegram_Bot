package org.example.util.state;

import org.example.data_classes.enums.UserState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StateManager {
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    public UserState getUserState(Long userId) {
        return userStates.getOrDefault(userId, UserState.START);
    }

    public void setUserState(Long userId, UserState state) {
        userStates.put(userId, state);
    }

    public void removeUserState(Long userId) {
        userStates.remove(userId);
    }

    public boolean userStateExists(Long userId) {
        return userStates.containsKey(userId);
    }
}
