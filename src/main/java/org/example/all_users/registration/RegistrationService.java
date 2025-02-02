package org.example.all_users.registration;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.dto.ChatBotResponse;
import org.example.data_classes.enums.UserState;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramApiQueue;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UpdateUtil updateUtil;
    private final StateManager stateManager;
    private final TelegramApiQueue telegramApiQueue;
    private final AdminRegistration adminRegistration;
    private final UserRegistration userRegistration;

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            Long chatId = updateUtil.getChatId(update);
            UserState currentState = stateManager.getUserState(chatId);
            processRegistration(update, currentState);
        }
    }

    private void processRegistration(Update update, UserState state) {
        switch (state) {
            //user
            case ENTERING_FIRSTNAME -> userRegistration.firstNameCheck(update);
            case ENTERING_LASTNAME -> userRegistration.lastNameCheck(update);
            case ENTERING_STUDY_PLACE -> userRegistration.studyPlaceCheck(update);
            case ENTERING_MIDDLE_NAME -> userRegistration.middleNameCheck(update);
            case ENTERING_EMAIL -> userRegistration.emailCheck(update);
            case ENTERING_PHONE_NUMBER -> userRegistration.phoneNumberCheck(update);

            //admin
            case ENTERING_SPECIAL_KEY -> adminRegistration.adminPasswordCheck(update);

            //all
            case CHOOSING_ROLE -> roleChooser(update);
            default -> startRegisterNewUser(update);
        }
    }

    private void startRegisterNewUser(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (updateUtil.getAdmin(update).isPresent()) {
            userRegistration.startRegistration(update);
        } else if (update.hasMessage() && update.getMessage().getFrom() != null) {

            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton("Админ"));
            row.add(new KeyboardButton("Пользователь"));

            ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                    .resizeKeyboard(true)
                    .keyboardRow(row)
                    .oneTimeKeyboard(true)
                    .build();

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Давайте начнём регистрацию!
                            
                            ps: чтобы стать администратором вам нужен специальный ключ!""")
                    .replyMarkup(keyboardMarkup)
                    .build()));
            stateManager.setUserState(chatId, UserState.CHOOSING_ROLE);
        }
    }

    private void roleChooser(Update update) {
        String userMessage = update.getMessage().getText();

        switch (userMessage) {
            case "Админ" -> adminRegistration.startRegistration(update);
            case "Пользователь" -> userRegistration.startRegistration(update);
        }
    }
}
