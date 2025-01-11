package org.example.telegram;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.admin.AdminUserService;
import org.example.base_user.BaseUserService;
import org.example.dto.ChatBotResponse;
import org.example.entity.UserState;
import org.example.entity.Role;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.util.UpdateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final CommandProcessingService commandProcessingService;
    private final UserRepository userRepository;
    private final StateManager stateManager = new StateManager();
    private final BaseUserService baseUserService;
    private final AdminUserService adminUserService;
    private final TelegramApiQueue telegramApiQueue;
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
            Long chatId = updateUtil.getChatId(update);
            UserState currentState = stateManager.getUserState(chatId);
            processMessage(update, currentState);
        }
    }

    private void processMessage(Update update, UserState state) {
        if (updateUtil.getUser(update).isEmpty()) {
            processRegistration(update, state);
        } else {
            processTextMessage(update);
        }
    }

    private void processRegistration(Update update, UserState state) {
        switch (state) {
            case START:
                handleStartState(update);
                break;

            case REGISTRATION:
                startRegisterNewUser(update);
                break;

            case CHOOSING_ROLE:
                roleChooser(update);
                break;

            case ENTERING_SPECIAL_KEY:
                adminPasswordCheck(update);
                break;

            default:
                processTextMessage(update);
                break;
        }
    }

    private void processTextMessage(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Role userRole = userRepository.findByChatId(chatId).get().getRole();

        switch (userRole) {
            case ADMIN -> adminUserService.onUpdateRecieved(update);
            case USER -> baseUserService.onUpdateRecieved(update);
        }
        ;
    }

    private void handleStartState(Update update) {
        Optional<Usr> usrOptional = updateUtil.getUser(update);

        if (usrOptional.isPresent()) {
            Usr usr = usrOptional.get();
            System.out.println("Пользователь уже зарегистрирован: " + usr.getUsername());

            processTextMessage(update);
        } else {
            startRegisterNewUser(update);
        }
    }

    private void startRegisterNewUser(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Давайте начнём регистрацию!
                            
                            Выберите свою роль:
                            (admin)
                            (usr)
                            
                            ps: чтобы стать администратором вам нужен специальный ключ!""")
                    .build()));
            stateManager.setUserState(chatId, UserState.CHOOSING_ROLE);
        }
    }

    private void roleChooser(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();

        switch (userMessage) {
            case "admin":
                telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Введите специальный ключ:""")
                        .build()));
                stateManager.setUserState(chatId, UserState.ENTERING_SPECIAL_KEY);
                break;

            case "usr":
                commandProcessingService.saveNewUser(update, Role.USER);
                stateManager.removeUserState(chatId);
                processTextMessage(update);
                break;

            default:
                telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Введите корректные данные.""")
                        .build()));
        }
    }

    private void adminPasswordCheck(Update update) {
        String message = update.getMessage().getText();
        Long chatId = updateUtil.getChatId(update);

        if (message.equals("1234")) {
            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ключ верный.""")
                    .build()));
            commandProcessingService.saveNewUser(update, Role.ADMIN);

            stateManager.removeUserState(chatId);
            processTextMessage(update);
        } else {
            telegramApiQueue.addResponse(new ChatBotResponse(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Ключ неверный.
                            
                            Начните регистрацию заново)""")
                    .build()));
            startRegisterNewUser(update);
        }
    }
}