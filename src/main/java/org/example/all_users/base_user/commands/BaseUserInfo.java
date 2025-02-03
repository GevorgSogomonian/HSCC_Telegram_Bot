package org.example.all_users.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.UsrExtraInfo;
import org.example.data_classes.enums.UserState;
import org.example.data_classes.data_base.entity.Usr;
import org.example.repository.UsrExtraInfoRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaseUserInfo {
    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final UsrExtraInfoRepository usrExtraInfoRepository;

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

            InlineKeyboardButton editButton = new InlineKeyboardButton("Редактировать");
            editButton.setCallbackData("user-edit_offer-editing");
            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(editButton))
                    .build();

            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .replyMarkup(inlineKeyboardMarkup)
                    .text(" ")
                    .build();

            if (user.getIsHSEStudent()) {
                sendMessage.setText(String.format("""
                                    Имя: *%s*
                                    Фамилия: *%s*
                                    ID: *%s*
                                    *%s*""",
                        user.getFirstName(),
                        user.getLastName(),
                        user.getUserId(),
                        user.getIsHSEStudent() ? "Студент ВШЭ" : "Не студент ВШЭ"));
            } else {
                UsrExtraInfo usrExtraInfo = usrExtraInfoRepository.findByChatId(chatId).get();

                sendMessage.setText(String.format("""
                                    Имя: *%s*
                                    Фамилия: *%s*
                                    Отчество: *%s*
                                    ID: *%s*
                                    Номер: *%s*
                                    Почта: *%s*
                                    *%s*""",
                        user.getFirstName(),
                        user.getLastName(),
                        usrExtraInfo.getMiddleName(),
                        user.getUserId(),
                        usrExtraInfo.getPhoneNumber(),
                        usrExtraInfo.getEmail(),
                        user.getIsHSEStudent() ? "Студент ВШЭ" : "Не студент ВШЭ"));
            }
            telegramSender.sendText(chatId, sendMessage);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Пользователь не найден.""")
                    .build());
        }
    }
}
