package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.Admin;
import org.example.data_classes.enums.UserState;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
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
        KeyboardButton keyboardButton3 = new KeyboardButton("Сообщение всем");
        KeyboardButton keyboardButton4 = new KeyboardButton("Архив");
        KeyboardButton keyboardButton5 = new KeyboardButton("Режим пользователя");
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();
        KeyboardRow row5 = new KeyboardRow();

        row1.add(keyboardButton1);
        row2.add(keyboardButton2);
        row3.add(keyboardButton3);
        row4.add(keyboardButton4);
        row5.add(keyboardButton5);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row2);
        keyboardRows.add(row1);
        keyboardRows.add(row3);
        keyboardRows.add(row4);
        keyboardRows.add(row5);

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
