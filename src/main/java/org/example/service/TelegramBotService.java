package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.entity.UserState;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.example.util.UpdateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final RegistrationUserService registrationUserService;
    private final UserRepository userRepository;
    private final BaseUserService baseUserService;
    private final AdminUserService adminUserService;
    private final UpdateUtil updateUtil;

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
            registrationCheck(update);
        }
    }

    private void registrationCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Optional<Usr> userOptional = userRepository.findByChatId(chatId);

        if (userOptional.isEmpty() || !userOptional.get().getRegistered()) {
            registrationUserService.onUpdateRecieved(update);
        } else {
            processMessage(update);
        }
    }

    private void processMessage(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Role userRole = userRepository.findByChatId(chatId).get().getRole();

        switch (userRole) {
            case ADMIN -> adminUserService.onUpdateRecieved(update);
            case USER -> baseUserService.onUpdateRecieved(update);
        }
    }
}