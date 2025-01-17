package org.example.all_users;

import lombok.RequiredArgsConstructor;
import org.example.admin.AdminUserService;
import org.example.all_users.registration.RegistrationService;
import org.example.all_users.registration.UserRegistration;
import org.example.base_user.BaseUserService;
import org.example.entity.Admin;
import org.example.entity.Role;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AllUserStart {

    private final UpdateUtil updateUtil;
    private final UserRepository userRepository;
    private final AdminUserService adminUserService;
    private final BaseUserService baseUserService;
    private final UserRegistration userRegistration;
    private final RegistrationService registrationService;

    public void handleStartState(Update update) {
        processTextMessage(update);
    }

    private void processTextMessage(Update update) {
//        Long chatId = updateUtil.getChatId(update);
        Optional<Admin> adminOptional = updateUtil.getAdmin(update);
        Optional<Usr> userOptional = updateUtil.getUser(update);
//        Role userRole = userRepository.findByChatId(chatId).get().getRole();

        if (adminOptional.isPresent() && userOptional.isEmpty() && adminOptional.get().getUserMode()) {
            registrationService.onUpdateReceived(update);
        } else if (adminOptional.isEmpty() && userOptional.isPresent()) {
            baseUserService.onUpdateRecieved(update);
        } else if (adminOptional.isPresent() && userOptional.isPresent() && adminOptional.get().getUserMode()) {
            baseUserService.onUpdateRecieved(update);
        } else if (adminOptional.isPresent() && !adminOptional.get().getUserMode()) {
            adminUserService.onUpdateRecieved(update);
        }
//        switch (userRole) {
//            case ADMIN -> adminUserService.onUpdateRecieved(update);
//            case USER -> baseUserService.onUpdateRecieved(update);
//        }
    }
}
