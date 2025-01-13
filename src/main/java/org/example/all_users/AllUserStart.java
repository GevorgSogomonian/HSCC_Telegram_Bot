package org.example.all_users;

import lombok.RequiredArgsConstructor;
import org.example.admin.AdminUserService;
import org.example.base_user.BaseUserService;
import org.example.entity.Role;
import org.example.repository.UserRepository;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
public class AllUserStart {

    private final UpdateUtil updateUtil;
    private final UserRepository userRepository;
    private final AdminUserService adminUserService;
    private final BaseUserService baseUserService;

    public void handleStartState(Update update) {
        processTextMessage(update);
    }

    private void processTextMessage(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Role userRole = userRepository.findByChatId(chatId).get().getRole();

        switch (userRole) {
            case ADMIN -> adminUserService.onUpdateRecieved(update);
            case USER -> baseUserService.onUpdateRecieved(update);
        }
    }
}
