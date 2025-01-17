package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.entity.Admin;
import org.example.entity.UserState;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramSender;
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
public class AdminStart {
    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;

    public void handleStartState(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Admin admin = updateUtil.getAdmin(update).get();

        System.out.println("Пользователь-админ уже зарегистрирован: " + admin.getUsername());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text("""
                выберите действие""")
                .build();

        KeyboardButton keyboardButton1 = new KeyboardButton("Новое мероприятие");
        KeyboardButton keyboardButton2 = new KeyboardButton("Все мероприятия");
        KeyboardButton keyboardButton3 = new KeyboardButton("Режим пользователя");
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();

        row1.add(keyboardButton1);
        row2.add(keyboardButton2);
        row3.add(keyboardButton3);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row2);
        keyboardRows.add(row1);
        keyboardRows.add(row3);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
        sendMessage.setReplyMarkup(keyboardMarkup);
        telegramSender.sendText(chatId, sendMessage);

        stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
    }
}
