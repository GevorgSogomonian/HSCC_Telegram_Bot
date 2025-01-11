package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotRequest;
import org.example.entity.BotState;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminNewEvent {

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final EventRepository eventRepository;
    private final TelegramApiQueue telegramApiQueue;
    private final AdminStart adminStart;

    public void handleNewEventCommand(Update update) {
        Long chatId = updateUtil.getChatId(update);

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                Введите название мероприятия:""")
                .build());

        stateManager.setUserState(chatId, BotState.ENTERING_EVENT_NAME);
    }

    public void eventNameCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int maxNameLength = 120;

        if (eventRepository.findByEventName(userMessage).isPresent()) {
            sendMessage.setText(String.format("""
                    Мероприятие с названием: %s уже существует.
                    
                    В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""",
                    userMessage));
            telegramSender.sendText(chatId, sendMessage);
        } else if (userMessage.isBlank()) {
            sendMessage.setText("""
                    Название мероприятия не может быть пустым(""");
            telegramSender.sendText(chatId, sendMessage);
        } else if (userMessage.length() > maxNameLength) {
            sendMessage.setText(String.format("""
                    Название мероприятия не может быть больше %s символов(""",
                    maxNameLength));
            telegramSender.sendText(chatId, sendMessage);
        } else {
            Event newEvent = new Event();
            newEvent.setCreatorChatId(chatId);
            newEvent.setEventName(userMessage);

            eventRepository.save(newEvent);
            sendMessage.setText("""
                    Отлично! Название сохранено!""");
            telegramSender.sendText(chatId, sendMessage);
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Введите описание мероприятия:""")
                    .build());
            stateManager.setUserState(chatId, BotState.ENTERING_EVENT_DESCRIPTION);
        }
    }

    public void eventDescriptionCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        int maxDescriptioonLength = 2000;

        Optional<Event> eventOptional = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId);

        if (userMessage.isBlank()) {
            sendMessage.setText("""
                    Описание мероприятия не может быть пустым(""");
            telegramSender.sendText(chatId, sendMessage);
        } else if (userMessage.length() > maxDescriptioonLength) {
            sendMessage.setText(String.format("""
                    Описание мероприятия не может быть больше %s символов(""",
                    maxDescriptioonLength));
            telegramSender.sendText(chatId, sendMessage);
        } else {
            Event event = eventOptional.get();
            event.setDescription(userMessage);
            eventRepository.save(event);
            sendMessage.setText("""
                    Отлично! Описание мероприятия сохранено!""");
            telegramSender.sendText(chatId, sendMessage);
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Пришлите обложку мероприятия:""")
                    .build());
            stateManager.setUserState(chatId, BotState.ENTERING_EVENT_PICTURE);
        }
    }

    public void eventPictureCheck(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.getMessage().hasDocument()) {
            String fileId = updateUtil.getFileId(update);
            try {
                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("Изображение успешно сохранено.")
                        .build());

                stateManager.setUserState(chatId, BotState.COMMAND_CHOOSING);
                adminStart.handleStartState(update);
            } catch (Exception e) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("Ошибка при добавлении запроса в очередь: " + e.getMessage())
                        .build());
            }
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("Пожалуйста, отправьте изображение для обложки мероприятия файлом.")
                    .build());
        }
    }
}
