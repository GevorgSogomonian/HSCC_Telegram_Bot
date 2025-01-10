package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Role;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
public class MessageDistributor {
    private final UpdateUtil updateUtil;
    private final AdminUserService adminUserService;
    private final BaseUserService baseUserService;

    public void processTextMessage(Update update) {
        Role userRole = updateUtil.getUser(update).get().getRole();

        switch (userRole) {
            case ADMIN -> adminUserService.onUpdateRecieved(update);
            case USER -> baseUserService.onUpdateRecieved(update);
        }
    }
}
