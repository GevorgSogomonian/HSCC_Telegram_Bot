package org.example.all_users;

import lombok.RequiredArgsConstructor;
import org.example.all_users.admin.AdminUserService;
import org.example.all_users.registration.RegistrationService;
import org.example.all_users.base_user.BaseUserService;
import org.example.data_classes.data_base.entity.Admin;
import org.example.data_classes.data_base.entity.Usr;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AllUserStart {

    private final UpdateUtil updateUtil;
    private final AdminUserService adminUserService;
    private final BaseUserService baseUserService;
    private final RegistrationService registrationService;

    public void handleStartState(Update update) {
        processTextMessage(update);
    }

    private void processTextMessage(Update update) {
        Optional<Admin> adminOptional = updateUtil.getAdmin(update);
        Optional<Usr> userOptional = updateUtil.getUser(update);

        if (adminOptional.isPresent() && userOptional.isEmpty() && adminOptional.get().getUserMode()) {
            registrationService.onUpdateReceived(update);
        } else if (adminOptional.isEmpty() && userOptional.isPresent()) {
            baseUserService.onUpdateRecieved(update);
        } else if (adminOptional.isPresent() && userOptional.isPresent() && adminOptional.get().getUserMode()) {
            baseUserService.onUpdateRecieved(update);
        } else if (adminOptional.isPresent() && !adminOptional.get().getUserMode()) {
            adminUserService.onUpdateRecieved(update);
        }
    }
}
