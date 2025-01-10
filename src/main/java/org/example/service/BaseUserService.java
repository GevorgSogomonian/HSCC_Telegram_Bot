package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotResponse;
import org.example.entity.BotState;
import org.example.entity.Event;
import org.example.entity.Usr;
import org.example.repository.EventRepository;
import org.example.repository.UserRepository;
import org.example.state_manager.StateManager;
import org.example.telegram_api.TelegramApiQueue;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.InputStream;
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
    private final UpdateUtil updateUtil;

    private final Map<String, Function<Update, List<SendMessage>>> commandHandlers = new HashMap<>();
    private final StateManager stateManager = new StateManager();
    private final ImageService imageService;
    private final TelegramApiQueue telegramApiQueue;

    public List<SendMessage> onUpdateRecieved(Update update) {
        List<SendMessage> responseMessageList = new ArrayList<>();

        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = updateUtil.getChatId(update);
//            String userMessage = update.getMessage().getText();
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

        List<Event> allEvents = eventRepository.findAll();

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();

            sendPhoto.setCaption(String.format("""
                    *%s*:
                    
                    %s""",
                    event.getEventName(), event.getDescription()));
            InputStream fileStream = null;
            try {
                fileStream = imageService.getFile("pictures", event.getImageUrl());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            InputFile inputFile = new InputFile(fileStream, event.getImageUrl());
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(inputFile);
            sendPhoto.setParseMode("Markdown");

            InlineKeyboardButton button = new InlineKeyboardButton("Подписаться");
            button.setCallbackData("регистрация события в google calendar");

            InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(button))
                    .build();

            sendPhoto.setReplyMarkup(keyboardMarkup);

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendPhoto));
        });

//        sendMessageList.addAll(handleStartState(update));
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
