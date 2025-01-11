package org.example.telegram;

import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
@Transactional
public class CommandProcessingService {
    private final UserRepository userRepository;

    public void saveNewUser(Update update, Role role) {
        Long chatId = update.getMessage().getChatId();
        org.telegram.telegrambots.meta.api.objects.User fromUser = update.getMessage().getFrom();

        Usr newUsr = new Usr();
        newUsr.setChatId(chatId);
        newUsr.setUsername(fromUser.getUserName());
        newUsr.setFirstName(fromUser.getFirstName());
        newUsr.setLastName(fromUser.getLastName());
        newUsr.setRole(role);
        newUsr.setLanguageCode(fromUser.getLanguageCode());
        newUsr.setIsPremium(fromUser.getIsPremium());
        newUsr.setIsBot(fromUser.getIsBot());

        userRepository.save(newUsr);
    }
}
