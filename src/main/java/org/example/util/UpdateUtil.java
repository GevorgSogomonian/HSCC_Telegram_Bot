package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.entity.Admin;
import org.example.entity.Usr;
import org.example.repository.AdminRepository;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateUtil {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public Long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else {
            return update.getMessage().getChatId();
        }
    }

    public String getFileId(Update update) {
        if (update.getMessage().hasPhoto()) {
            return  update.getMessage().getPhoto().get(0).getFileId();
        } else if (update.getMessage().hasDocument()) {
            return update.getMessage().getDocument().getFileId();
        } else {
            return "";
        }
    }

    public Optional<Usr> getUser(Update update) {
        Long chatId = getChatId(update);
        return userRepository.findByChatId(chatId);
    }

    public Optional<Admin> getAdmin(Update update) {
        Long chatId = getChatId(update);
        return adminRepository.findByChatId(chatId);
    }
}