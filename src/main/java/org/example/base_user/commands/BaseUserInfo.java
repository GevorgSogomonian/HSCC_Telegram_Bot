package org.example.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaseUserInfo {
    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;

    public void handleUserInfoCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        sendUserInfo(update);

        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
    }

    private void sendUserInfo(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Optional<Usr> userOptional = updateUtil.getUser(update);

        if (userOptional.isPresent()) {
            Usr user = userOptional.get();
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text(String.format("""
                                    Имя: *%s*
                                    Фамилия: *%s*
                                    Мероприятий посетил: *%s*
                                    Мероприятий хотел посетить, но пропустил: *%s*
                                    ID: *%s*""",
                                    user.getFirstName(),
                                    user.getLastName(),
                                    user.getNumberOfVisitedEvents(),
                                    user.getNumberOfMissedEvents(),
                                    user.getUserId()))
                    .build());
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Пользователь не найден.""")
                    .build());
        }
    }
}
