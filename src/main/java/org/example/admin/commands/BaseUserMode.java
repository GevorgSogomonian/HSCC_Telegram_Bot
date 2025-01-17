package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.all_users.AllUserStart;
import org.example.base_user.commands.BaseStart;
import org.example.entity.Admin;
import org.example.entity.Usr;
import org.example.repository.AdminRepository;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

@Service
@RequiredArgsConstructor
public class BaseUserMode {

    private final UpdateUtil updateUtil;
//    private final AllUserStart allUserStart;
    private final AdminRepository adminRepository;
    private final TelegramSender telegramSender;
    private final BaseStart baseStart;

    public void handleBaseUserMode(Update update) {
        Admin admin = updateUtil.getAdmin(update).get();
        Long chatId = updateUtil.getChatId(update);

        admin.setUserMode(true);
        adminRepository.save(admin);

        if (updateUtil.getUser(update).isEmpty()) {
            ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .replyMarkup(replyKeyboardRemove)
                            .text("""
                                    Сначала, вы должны зарегистрировать пользовательский аккаунт.
                                    
                                    Чтобы активировать процесс регистрации, пришлите любое сообщение.""")
                    .build());
        } else {
            baseStart.handleStartState(update);
        }
//        allUserStart.handleStartState(update);
    }
}
