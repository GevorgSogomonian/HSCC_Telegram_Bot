package org.example.all_users.registration;

import lombok.RequiredArgsConstructor;
import org.example.admin.AdminUserService;
import org.example.dto.ChatBotResponse;
import org.example.entity.Admin;
import org.example.entity.UserState;
import org.example.repository.AdminRepository;
import org.example.repository.UserRepository;
import org.example.state_manager.StateManager;
import org.example.util.UserUtilService;
import org.example.telegram.api.TelegramApiQueue;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Service
@RequiredArgsConstructor
public class AdminRegistration {

    private final TelegramApiQueue telegramApiQueue;
    private final UpdateUtil updateUtil;
    private final StateManager stateManager;
    private final UserUtilService userUtilService;
    private final AdminUserService adminUserService;
    private final AdminRepository adminRepository;

    public void startRegistration(Update update) {
        Long chatId = updateUtil.getChatId(update);

        telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Введите специальный ключ:""")
                .build()));
        stateManager.setUserState(chatId, UserState.ENTERING_SPECIAL_KEY);
    }

    public void adminPasswordCheck(Update update) {
        String message = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        if (message.equals("1234")) {
            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ключ верный.""")
                    .build()));
            Admin admin = userUtilService.getNewAdmin(update);
            adminRepository.save(admin);

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Поздравляю, вы зарегистрированы!""")
                    .build()));

            stateManager.removeUserState(chatId);
            adminUserService.onUpdateRecieved(update);
        } else {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton("Админ"));
            row.add(new KeyboardButton("Пользователь"));

            ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .oneTimeKeyboard(true)
                    .keyboardRow(row)
                    .build();

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ключ неверный.
                            
                            Начните регистрацию заново)""")
                    .replyMarkup(keyboardMarkup)
                    .build()));

            stateManager.setUserState(chatId, UserState.CHOOSING_ROLE);
        }
    }
}
