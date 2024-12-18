package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.entity.BotState;
import org.example.entity.Role;
import org.example.entity.Usr;
import org.example.repository.UserRepository;
import org.example.state_manager.StateManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.example.entity.Role.ADMIN;
import static org.example.entity.Role.USER;

@Service
@RequiredArgsConstructor
public class TelegramBotService extends TelegramLongPollingBot {

    private final Map<String, String> waitingForInput = new ConcurrentHashMap<>();
    private final CommandProcessingService commandProcessingService;
    private final UserRepository userRepository;
    private final StateManager stateManager = new StateManager();

    @Value("${spring.telegram.bot.username}")
    private String botUsername;

    @Value("${spring.telegram.bot.token}")
    private String botToken;

    private final Map<BotState, Consumer<Update>> commandHandlers = new HashMap<>();

    @PostConstruct
    public void init() {
        System.out.println("Username: " + botUsername);
        System.out.println("Token: " + botToken);

//        //Для администратора
//        commandHandlers.put(BotState.START, this::handleSearchCommand);
//        commandHandlers.put(BotState.REGISTRATION, this::startRegisterNewUser);
//
//        //Для обычных пользователей
//        commandHandlers.put("Доступные мероприятия", this::handleMostPersonalCommand);
//        commandHandlers.put("Мои мероприятия", this::handlePersonalCommand);
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String userMessage = update.getMessage().getText();
            BotState currentState = stateManager.getUserState(chatId);
            SendMessage replyMessage = processMessage(update, currentState);

//            if (waitingForInput.containsKey(chatId.toString())) {
//                String pendingCommand = waitingForInput.remove(chatId.toString());
//                if (pendingCommand.equals("search")) {
//                    processSearchQuery(update);
//                }
//                return;
//            }

//            commandHandlers.getOrDefault(userMessage, this::handleUnknownCommand).accept(update);
        }
    }

    private SendMessage processMessage(Update update, BotState state) {
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());

        switch (state) {
            case CHOOSING_ROLE:
                roleChooser(update);
                break;

            case ENTERING_SPECIAL_KEY:
                adminPasswordCheck(update);
                break;

            default:
                userRepository.findByChatId(chatId).ifPresentOrElse(
                        usr -> System.out.println("Пользователь уже зарегистрирован: " + usr.getUsername()),
                        () -> startRegisterNewUser(update)
                );
//                message.setText("Что-то пошло не так. Попробуй начать заново с команды /start.");
//                stateManager.removeUserState(chatId);
        }

        return message;
    }

    private String truncateDescription(String description) {
        int maxLength = 500;
        if (description != null && description.length() > maxLength) {
            return description.substring(0, maxLength) + "...";
        }
        return description != null ? description : "Описание недоступно.";
    }

    private void handleUnknownCommand(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId().toString());
        message.setText("Выберите действие:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🌀 Случайный фильм"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("🎬 Популярные фильмы"));

        keyboardRows.add(row1);
        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSplitResponse(String chatId, String text) {
        int maxMessageLength = 4096;
        for (int i = 0; i < text.length(); i += maxMessageLength) {
            String part = text.substring(i, Math.min(text.length(), i + maxMessageLength));
            sendResponse(chatId, part);
        }
    }

    private void sendResponse(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void startRegisterNewUser(Update update) {
        Long chatId = update.getMessage().getChatId();

        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            org.telegram.telegrambots.meta.api.objects.User fromUser = update.getMessage().getFrom();

            sendSplitResponse(chatId.toString(), """
                    Давайте начнём регистрацию!
                    
                    Выберите свою роль:
                    (admin)
                    (usr)
                
                    ps: чтобы стать администратором вам нужен специальный ключ!""");

            stateManager.setUserState(chatId, BotState.CHOOSING_ROLE);
        }
    }

    private void roleChooser(Update update) {
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        switch (userMessage) {
            case "admin":
                sendSplitResponse(chatId.toString(), """
                Введите специальный ключ:""");
                stateManager.setUserState(chatId, BotState.ENTERING_SPECIAL_KEY);
                break;

            case "usr":
                commandProcessingService.saveNewUser(update, Role.USER);
                stateManager.removeUserState(chatId);
                break;

            default:
                sendSplitResponse(chatId.toString(), """
                Введите корректные данные.""");
        }
    }

    private void adminPasswordCheck(Update update) {
        String message = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        switch (message) {
            case "1234":
                sendSplitResponse(chatId.toString(), """
                Ключ верный.""");
                commandProcessingService.saveNewUser(update, Role.ADMIN);

                stateManager.removeUserState(chatId);
                break;

            default:
                sendSplitResponse(chatId.toString(), """
                Ключ неверный.
                
                Начните регистрацию заново)""");
                startRegisterNewUser(update);
        }
    }
}