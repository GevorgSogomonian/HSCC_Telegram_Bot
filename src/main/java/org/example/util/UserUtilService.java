package org.example.util;

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
public class UserUtilService {

    private final UpdateUtil updateUtil;
    private final UserRepository userRepository;

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
        newUser.setNumberOfVisitedEvents(0);
        newUser.setNumberOfMissedEvents(0);
        newUser.setSubscribedEventIds("");


        Long uniqueNumber = userRepository.getUniqueNumber();
        newUser.setUserId(getNewUserId(uniqueNumber));

        return newUser;
    }

    private Long getNewUserId(Long uniqueNumber) {
        Long bigNumber = 342_665L;
        return Long.parseLong(String.format("%s%s", bigNumber / uniqueNumber, bigNumber % uniqueNumber));
    }
}
