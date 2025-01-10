package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatBotRequest;
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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private final Map<String, Function<Update, List<SendMessage>>> commandHandlers = new HashMap<>();
    private final StateManager stateManager = new StateManager();
    private final ImageService imageService;
    private final TelegramApiQueue telegramApiQueue;
    private final UpdateUtil updateUtil;
//    private final TelegramPhotoSender telegramPhotoSender;

    public List<SendMessage> onUpdateRecieved(Update update) {
        List<SendMessage> responseMessageList = new ArrayList<>();

        if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        } else if (update.hasMessage()) {
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
            case ENTERING_EVENT_PICTURE -> eventPictureCheck(update);
            case COMMAND_CHOOSING -> processTextMessage(update);
            default -> responseMessageList;
        };

        responseMessageList.forEach(responseMessage -> responseMessage.setChatId(chatId));

        return responseMessageList;
    }

    @PostConstruct
    public void init() {
        commandHandlers.put("Все мероприятия", this::handleAllEventsCommand);
        commandHandlers.put("Новое мероприятие", this::handleNewEventCommand);
    }

    private List<SendMessage> processTextMessage(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();

        return commandHandlers.getOrDefault(userMessage, this::handleStartState).apply(update);
    }

    private List<SendMessage> handleAllEventsCommand(Update update) {
        Long chatId = update.getMessage().getChatId();
        List<SendMessage> sendMessageList = new ArrayList<>();
        List<Event> allEvents = eventRepository.findAll();

        allEvents.forEach(event -> {
            SendPhoto sendPhoto = new SendPhoto();

            InputStream fileStream = null;
            try {
                fileStream = imageService.getFile("pictures", event.getImageUrl());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            InputFile inputFile = new InputFile(fileStream, event.getImageUrl());
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(inputFile);

            sendPhoto.setCaption(String.format("""
                    *%s*:
                    
                    %s""",
                    event.getEventName(), event.getDescription()));

            InlineKeyboardButton editButton = new InlineKeyboardButton("редактировать");
            editButton.setCallbackData("edit_event_" + event.getId());

            InlineKeyboardButton deleteButton = new InlineKeyboardButton("удалить");
            deleteButton.setCallbackData("delete_event_" + event.getId());

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(editButton))
                    .keyboardRow(List.of(deleteButton))
                    .build();

            sendPhoto.setReplyMarkup(inlineKeyboardMarkup);

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, sendPhoto));
            log.info("try to send image from minio. Image Name");
        });

//        sendMessageList.addAll(handleStartState(update));
        return sendMessageList;
    }

    private List<SendMessage> handleNewEventCommand(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();

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
            stateManager.setUserState(chatId, BotState.ENTERING_EVENT_PICTURE);
        }


        sendMessageList.add(sendMessage);
        sendMessageList.add(SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Пришлите обложку мероприятия:""")
                .build());

//        sendMessageList.add(sendMessage);
//        sendMessageList.add(SendMessage.builder()
//                        .chatId(chatId)
//                        .text("""
//                                Поздравляю! Вы создали новое мероприятие!
//
//                                Теперь его смогут увидеть обычные пользователи.""")
//                .build());

//        sendMessageList.addAll(handleStartState(update));
        return sendMessageList;
    }

    private List<SendMessage> eventPictureCheck(Update update) {
        List<SendMessage> sendMessageList = new ArrayList<>();
        Long chatId = update.getMessage().getChatId();

        if (update.hasMessage()) {
            String fileType = "";
            String fileId = "";
            if (update.getMessage().hasPhoto()) {
                fileId = update.getMessage().getPhoto().get(0).getFileId();
            } else if (update.getMessage().hasDocument()) {
                fileId = update.getMessage().getDocument().getFileId();
                fileType = update.getMessage().getDocument().getMimeType();
            }
            if (fileType == null) {
                sendMessageList.add(SendMessage.builder()
                                .chatId(chatId)
                                .text("""
                                        Отправьте изображение файлом или быстрым способом!""")
                        .build());
                return sendMessageList;
            }

            try {
                // Добавляем запрос на получение файла в очередь
                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));

                // Обработка ответа должна происходить асинхронно в другом месте,
                // например, в обработчике очереди
                sendMessageList.add(SendMessage.builder()
                        .chatId(chatId)
                        .text("Запрос на получение изображения отправлен. Пожалуйста, подождите.")
                        .build());

                stateManager.setUserState(chatId, BotState.COMMAND_CHOOSING);
            } catch (Exception e) {
                sendMessageList.add(SendMessage.builder()
                        .chatId(chatId)
                        .text("Ошибка при добавлении запроса в очередь: " + e.getMessage())
                        .build());
            }
        } else {
            sendMessageList.add(SendMessage.builder()
                    .chatId(chatId)
                    .text("Пожалуйста, отправьте изображение для обложки мероприятия.")
                    .build());
        }

        return sendMessageList;
    }

    private String getMimeTypeByExtension(String filePath) {
        Map<String, String> mimeTypes = Map.of(
                "jpg", "image/jpeg",
                "png", "image/png",
                "gif", "image/gif",
                "pdf", "application/pdf",
                "txt", "text/plain"
        );

        String extension = filePath.substring(filePath.lastIndexOf('.') + 1);
        return mimeTypes.get(extension);
    }

    private void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith("delete_event_")) {
            System.out.println("сработало удаление");
            Long eventId = Long.parseLong(callbackData.split("_")[2]);
            handleDeleteEvent(chatId, eventId);
        } else if (callbackData.startsWith("edit_event_")) {
            Long eventId = Long.parseLong(callbackData.split("_")[2]);
            handleEditEvent(chatId, eventId);
        } else {
            // Обработка других callback-запросов
            sendUnknownCallbackResponse(chatId);
        }

        // Отправляем подтверждение обработки callback-запроса
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        answer.setText("Команда обработана.");

        telegramApiQueue.addResponse(new ChatBotResponse(chatId, answer));
    }

    private void handleDeleteEvent(Long chatId, Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();

            // Удаляем изображение из MinIO
            String imageUrl = event.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                String bucketName = "pictures"; // Название bucket (замените на ваше)
                String objectName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1); // Получаем имя объекта из URL
                imageService.deleteImage(bucketName, objectName);
            }

            // Удаляем мероприятие из базы данных
            eventRepository.delete(event);

            // Отправляем подтверждение пользователю
            SendMessage confirmationMessage = new SendMessage();
            confirmationMessage.setChatId(chatId.toString());
            confirmationMessage.setText("Мероприятие и его изображение успешно удалены!");

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, confirmationMessage));
        } else {
            // Отправляем сообщение, если мероприятие не найдено
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId.toString());
            errorMessage.setText("Мероприятие не найдено!");

            telegramApiQueue.addResponse(new ChatBotResponse(chatId, errorMessage));
        }
    }

    private void handleEditEvent(Long chatId, Long eventId) {
//        Optional<Event> eventOptional = eventRepository.findById(eventId);
//
//        if (eventOptional.isPresent()) {
//            SendMessage editMessage = new SendMessage();
//            editMessage.setChatId(chatId.toString());
//            editMessage.setText("Введите новые данные для мероприятия:");
//
//            stateManager.setUserState(chatId, BotState.EDITING_EVENT);
//            stateManager.setEventBeingEdited(chatId, eventId);
//
//            telegramApiQueue.addResponse(new ChatBotResponse(chatId, editMessage));
//        } else {
//            SendMessage errorMessage = new SendMessage();
//            errorMessage.setChatId(chatId.toString());
//            errorMessage.setText("Мероприятие не найдено!");
//
//            telegramApiQueue.addResponse(new ChatBotResponse(chatId, errorMessage));
//        }
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramApiQueue.addResponse(new ChatBotResponse(chatId, unknownCallbackMessage));
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
