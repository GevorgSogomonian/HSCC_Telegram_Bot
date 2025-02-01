package org.example.all_users.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.all_users.admin.commands.AdminStart;
import org.example.data_classes.data_base.entity.Admin;
import org.example.data_classes.enums.UserState;
import org.example.data_classes.data_base.entity.Usr;
import org.example.repository.AdminRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.helpers.UpdateUtil;
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
