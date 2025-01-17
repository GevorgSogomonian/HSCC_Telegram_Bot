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

        System.out.println("Пользователь уже зарегистрирован: " + usr.getUsername());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("""
                выберите действие""")
                .build();

        KeyboardButton keyboardButton1 = new KeyboardButton("Актуальные мероприятия");
        KeyboardRow row1 = new KeyboardRow();
        row1.add(keyboardButton1);

        KeyboardButton keyboardButton2 = new KeyboardButton("Мои мероприятия");
        KeyboardRow row2 = new KeyboardRow();
        row2.add(keyboardButton2);

        KeyboardButton keyboardButton3 = new KeyboardButton("Информация о себе");
        KeyboardRow row3 = new KeyboardRow();
        row3.add(keyboardButton3);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);

        if (usr.getIsAdminClone()) {
            KeyboardButton keyboardButton4 = new KeyboardButton("Вернуться в режим админа");
            KeyboardRow row4 = new KeyboardRow();
            row4.add(keyboardButton4);

            keyboardRows.add(row4);
        }

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();

        sendMessage.setReplyMarkup(keyboardMarkup);

        sendMessage.setChatId(chatId);
        telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendMessage));
        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
        System.out.printf("""
                userState: %s%n""", stateManager.getUserState(chatId));
    }
}
