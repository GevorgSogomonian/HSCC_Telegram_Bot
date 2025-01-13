package org.example.all_users.registration;

import org.example.entity.Usr;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TemporaryUserService {

    private final Map<Long, Usr> userTemporaryData = new ConcurrentHashMap<>();

    public void putTemporaryUserData(Usr newUser) {
        Long chatId = newUser.getChatId();
        userTemporaryData.put(chatId, newUser);
    }

    public Usr getTemporaryUserData(Long chatId) {
        return userTemporaryData.get(chatId);
    }
}
