package org.example.util.telegram;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.all_users.AllUserStart;
import org.example.all_users.registration.RegistrationService;
import org.example.data_classes.data_base.entity.Admin;
import org.example.data_classes.data_base.entity.Usr;
import org.example.repository.AdminRepository;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final RegistrationService registrationService;
    private final AllUserStart allUserStart;
    private final UpdateUtil updateUtil;
    private final AdminRepository adminRepository;

    @Value("${spring.telegram.bot.username}")
    private String botUsername;

    @Value("${spring.telegram.bot.token}")
    private String botToken;

    @PostConstruct
    public void init() {
        System.out.println("Username: " + botUsername);
        System.out.println("Token: " + botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            processMessage(update);
        }
    }

    private void processMessage(Update update) {
        Optional<Admin> adminOptional = updateUtil.getAdmin(update);
        Optional<Usr> userOptional = updateUtil.getUser(update);

        if (adminOptional.isEmpty() && userOptional.isEmpty()) {
            registrationService.onUpdateReceived(update);
        } else {
            allUserStart.handleStartState(update);
        }
    }
}