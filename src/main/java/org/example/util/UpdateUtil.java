package org.example.util;

import lombok.RequiredArgsConstructor;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateUtil {
    private final UserRepository userRepository;

    public Long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else {
            return update.getMessage().getChatId();
        }
    }

    public Optional<Usr> getUser(Update update) {
        Long chatId = getChatId(update);

        return userRepository.findByChatId(chatId);
    }
}