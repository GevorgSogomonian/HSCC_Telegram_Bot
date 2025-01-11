package org.example.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotResponse;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BaseStart {
    private final UpdateUtil updateUtil;
    private final TelegramApiQueue telegramApiQueue;
    private final StateManager stateManager;

    public void handleStartState(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Usr usr = updateUtil.getUser(update).get();

        System.out.println("Пользователь-админ уже зарегистрирован: " + usr.getUsername());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                выберите действие""",
                        usr.getFirstName()))
                .build();

        KeyboardButton keyboardButton1 = new KeyboardButton("Актуальные мероприятия");
        KeyboardButton keyboardButton2 = new KeyboardButton("Мои мероприятия");
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row1);
        keyboardRows.add(row2);

        row1.add(keyboardButton1);
        row2.add(keyboardButton2);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sendMessage.setReplyMarkup(keyboardMarkup);

        sendMessage.setChatId(chatId);
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendMessage));
        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
    }
}
