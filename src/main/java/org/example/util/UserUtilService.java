package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.entity.Usr;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
@Transactional
public class UserUtilService {

    private final UpdateUtil updateUtil;

    public Usr getNewUser(Update update, Role role) {
        Long chatId = updateUtil.getChatId(update);
        org.telegram.telegrambots.meta.api.objects.User fromUser = update.getMessage().getFrom();

        Usr newUser = new Usr();
        newUser.setChatId(chatId);
        newUser.setUsername(fromUser.getUserName());
        newUser.setRole(role);
        newUser.setLanguageCode(fromUser.getLanguageCode());
        newUser.setIsPremium(fromUser.getIsPremium());
        newUser.setIsBot(fromUser.getIsBot());

        return newUser;
    }
}
