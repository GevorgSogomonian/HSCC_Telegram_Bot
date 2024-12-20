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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class AdminUserService {

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
            case ENTERING_EVENT_NAME -> eventNameCheck(update);
            case ENTERING_EVENT_DESCRIPTION -> eventDescriptionCheck(update);
            case COMMAND_CHOOSING -> processTextMessage(update);
            default -> responseMessageList;
        };

        responseMessageList.forEach(responseMessage -> responseMessage.setChatId(chatId));

        return responseMessageList;
    }

    @PostConstruct
    public void init() {

        //Для администратора
        commandHandlers.put("Все мероприятия", this::handleAllEventsCommand);
        commandHandlers.put("Новое мероприятие", this::handleNewEventCommand);

//        commandHandlers.put(BotState.REGISTRATION, this::startRegisterNewUser);
//
//        //Для обычных пользователей
//        commandHandlers.put("Доступные мероприятия", this::handleMostPersonalCommand);
//        commandHandlers.put("Мои мероприятия", this::handlePersonalCommand);
    }

    private List<SendMessage> processTextMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        return commandHandlers.getOrDefault(userMessage, this::handleStartState).apply(update);
    }

    private List<SendMessage> handleAllEventsCommand(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        List<Event> allEvents = eventRepository.findAll();

        allEvents.forEach(event -> {
            SendMessage singleEventMessage = new SendMessage();

            InlineKeyboardButton button1 = new InlineKeyboardButton("редактировать");
            InlineKeyboardButton button2 = new InlineKeyboardButton("удалить");

            button1.setCallbackData("edit");
            button2.setCallbackData("delete");

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(button1))
                    .keyboardRow(List.of(button2))
                    .clearKeyboard()
                    .build();

            singleEventMessage.setReplyMarkup(inlineKeyboardMarkup);

            singleEventMessage.setText(String.format("""
                    *%s*:
                    
                    %s""",
                    event.getEventName(), event.getDescription()));

            sendMessageList.add(singleEventMessage);
        });

        sendMessageList.addAll(handleStartState(update));
        return sendMessageList;
    }

    private List<SendMessage> handleNewEventCommand(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();

        sendMessageList.add(SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                Введите название мероприятия:""")
                .build());

        stateManager.setUserState(chatId, BotState.ENTERING_EVENT_NAME);

        return sendMessageList;
    }

    private List<SendMessage> eventNameCheck(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int maxNameLength = 120;

        if (eventRepository.findByEventName(userMessage).isPresent()) {
            sendMessage.setText(String.format("""
                    Мероприятие с названием: %s уже существует.
                    
                    В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""",
                    userMessage));
        } else if (userMessage.isBlank()) {
            sendMessage.setText("""
                    Название мероприятия не может быть пустым(""");
        } else if (userMessage.length() > maxNameLength) {
            sendMessage.setText(String.format("""
                    Название мероприятия не может быть больше %s символов(""",
                    maxNameLength));
        } else {
            Event newEvent = new Event();
            newEvent.setCreatorChatId(chatId);
            newEvent.setEventName(userMessage);

            eventRepository.save(newEvent);
            sendMessage.setText("""
                    Отлично! Название сохранено!""");
            stateManager.setUserState(chatId, BotState.ENTERING_EVENT_DESCRIPTION);
        }

        sendMessageList.add(sendMessage);
        sendMessageList.add(SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Введите описание мероприятия:""")
                .build());

        return sendMessageList;
    }

    private List<SendMessage> eventDescriptionCheck(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();
        String userMessage = update.getMessage().getText();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int maxDescriptioonLength = 2000;

        Optional<Event> eventOptional = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId);

        if (userMessage.isBlank()) {
            sendMessage.setText("""
                    Описание мероприятия не может быть пустым(""");
        } else if (userMessage.length() > maxDescriptioonLength) {
            sendMessage.setText(String.format("""
                    Описание мероприятия не может быть больше %s символов(""",
                    maxDescriptioonLength));
        } else {
            Event event = eventOptional.get();
            event.setDescription(userMessage);

            eventRepository.save(event);
            sendMessage.setText("""
                    Отлично! Описание мероприятия сохранено!""");
            stateManager.setUserState(chatId, BotState.COMMAND_CHOOSING);
        }

        sendMessageList.add(sendMessage);
        sendMessageList.add(SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Поздравляю! Вы создали новое мероприятие!
                                
                                Теперь его смогут увидеть обычные пользователи.""")
                .build());
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

        KeyboardButton keyboardButton1 = new KeyboardButton("Новое мероприятие");
        KeyboardButton keyboardButton2 = new KeyboardButton("Все мероприятия");
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
