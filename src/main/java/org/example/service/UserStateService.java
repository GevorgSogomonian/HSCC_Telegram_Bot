package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.util.UpdateUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
@Scope("prototype")
@RequiredArgsConstructor
public class UserStateService {
    private final Map<Long, Queue<Consumer<Update>>> userActionsMap = new ConcurrentHashMap<>();
    private final Map<Long, Consumer<Update>> currentActionsMap = new ConcurrentHashMap<>();

    private final UpdateUtil updateUtil;

    public void executeNextAction(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Queue<Consumer<Update>> userActionsQueue = userActionsMap.get(chatId);

        if (userActionsQueue != null && !userActionsQueue.isEmpty()) {
            Consumer<Update> currentUserAction = currentActionsMap.get(chatId);

            // Если текущий шаг уже выполняется, не загружаем новый
            if (currentUserAction == null) {
                currentUserAction = userActionsQueue.peek();
                currentActionsMap.put(chatId, currentUserAction);
            }

            // Выполнение текущего действия
            if (currentUserAction != null) {
                currentUserAction.accept(update);
            }
        }
    }

    public void completeCurrentAction(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Queue<Consumer<Update>> actions = userActionsMap.get(chatId);

        if (actions != null) {
            actions.poll(); // Удаляем выполненный шаг
            currentActionsMap.remove(chatId); // Удаляем текущий шаг
        }

        // Выполняем следующий шаг
        executeNextAction(update);
    }

    public void start(Update update, Queue<Consumer<Update>> actions) {
        Long chatId = updateUtil.getChatId(update);
        userActionsMap.put(chatId, actions);
        executeNextAction(update);
    }

    public void addAction(Long chatId, Consumer<Update> action) {
        userActionsMap.get(chatId).add(action);
    }

    public void completeAllActions(Long chatId) {
        userActionsMap.get(chatId).clear();
    }

    public boolean checkEntry(Long chatId) {
        return userActionsMap.containsKey(chatId);
    }

    public interface UserStateManager {
        void endTask(Update update);
    }
}
