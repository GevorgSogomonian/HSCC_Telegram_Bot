package org.example.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.admin.commands.AdminStart;
import org.example.entity.Admin;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.repository.AdminRepository;
import org.example.state_manager.StateManager;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReturnToAdminMode {

    private final UpdateUtil updateUtil;
    private final AdminRepository adminRepository;
    private final StateManager stateManager;
    private final AdminStart adminStart;

    public void handleReturnToAdminModeCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Optional<Usr> userOptional = updateUtil.getUser(update);
        Optional<Admin> adminOptional = updateUtil.getAdmin(update);

        if (userOptional.isPresent() && adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            admin.setUserMode(false);
            adminRepository.save(admin);

            stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
            adminStart.handleStartState(update);
        }
    }
}
