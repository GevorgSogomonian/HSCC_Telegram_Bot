package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.entity.BotState;
import org.example.entity.Event;
import org.example.entity.Usr;
import org.example.repository.EventRepository;
import org.example.repository.UserRepository;
import org.example.state_manager.StateManager;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class BaseUserService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private final Map<String, Function<Update, List<SendMessage>>> commandHandlers = new HashMap<>();
    private final StateManager stateManager = new StateManager();

    public List<SendMessage> onUpdateRecieved(Update update) {
        List<SendMessage> responseMessageList = new ArrayList<>();

        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String userMessage = update.getMessage().getText();
            BotState currentState = stateManager.getUserState(chatId);
            responseMessageList = processMessage(update, currentState);
        }

        return responseMessageList;
    }

    public List<SendMessage> processMessage(Update update, BotState state) {
        Long chatId = update.getMessage().getChatId();

        List<SendMessage> responseMessageList = new ArrayList<>();

        responseMessageList = switch (state) {
            case START -> handleStartState(update);
            case COMMAND_CHOOSING -> processTextMessage(update);
            default -> responseMessageList;
        };

        responseMessageList.forEach(responseMessage -> responseMessage.setChatId(chatId));

        return responseMessageList;
    }

    @PostConstruct
    public void init() {
        commandHandlers.put("Актуальные мероприятия", this::handleActualEventsCommand);
    }

    private List<SendMessage> processTextMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        return commandHandlers.getOrDefault(userMessage, this::handleStartState).apply(update);
    }

    private List<SendMessage> handleActualEventsCommand(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        List<Event> eventList = eventRepository.findAll();
        eventList.forEach(event -> {
            sendMessageList.add(SendMessage.builder()
                            .chatId(chatId)
                            .text(event.toString())
                    .build());
        });

        sendMessageList.addAll(handleStartState(update));
        return sendMessageList;
    }

    private List<SendMessage> handleStartState(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        Usr usr = userRepository.findByChatId(chatId).get();

        System.out.println("Пользователь-админ уже зарегистрирован: " + usr.getUsername());

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                выберите действие""",
                        usr.getFirstName()))
                .build();

        KeyboardButton keyboardButton1 = new KeyboardButton("Актуальные мероприятия");
        KeyboardButton keyboardButton2 = new KeyboardButton("Мои мероприятия");
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        keyboardRows.add(row1);
        keyboardRows.add(row2);

        row1.add(keyboardButton1);
        row2.add(keyboardButton2);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();

        sendMessage.setReplyMarkup(keyboardMarkup);

        sendMessage.setChatId(chatId);
        sendMessageList.add(sendMessage);
        stateManager.setUserState(chatId, BotState.COMMAND_CHOOSING);
        return sendMessageList;
    }
}
