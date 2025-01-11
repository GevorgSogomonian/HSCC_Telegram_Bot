package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotRequest;
import org.example.entity.BotState;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.util.image.ImageService;
import org.example.state_manager.StateManager;
import org.example.telegram.api.TelegramApiQueue;
import org.example.telegram.api.TelegramSender;
import org.example.util.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.InputStream;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminEditEvent {

    private final EventRepository eventRepository;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final UpdateUtil updateUtil;
    private final ImageService imageService;
    private final TelegramApiQueue telegramApiQueue;
    private final AdminStart adminStart;

    public void handleEditEvent(Long chatId, Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Текущее название:""")
                    .build());
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                            *%s*""", eventOptional.get().getEventName()))
                    .build());
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Введите новое название для мероприятия:""")
                    .build());
            stateManager.setUserState(chatId, BotState.EDITING_EVENT_NAME);
            stateManager.setEventBeingEdited(chatId, eventId);
        } else {
            SendMessage errorMessage = new SendMessage();
            errorMessage.setChatId(chatId.toString());
            errorMessage.setText("Мероприятие не найдено!");

            telegramSender.sendText(chatId, errorMessage);
        }
    }

    public void checkEditedEventName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        int maxNameLength = 120;
        Long eventId = stateManager.getEventBeingEdited(chatId);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (userMessage.isBlank()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                        Название мероприятия не может быть пустым(""")
                    .build());
        } else if (userMessage.length() > maxNameLength) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Название мероприятия не может быть больше *%s* символов(""",
                            maxNameLength))
                    .build());
        } else if (eventOptional.isEmpty()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                        Мероприятие не найдено!""")
                    .build());
        } else if (!eventOptional.get().getId().equals(eventId)) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Мероприятие с названием: *%s* уже существует.
                    
                    В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""", userMessage))
                    .build());
        } else {
            Event editedEvent = eventOptional.get();
            editedEvent.setEventName(userMessage);

            eventRepository.save(editedEvent);
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                        Отлично! Название сохранено!""")
                    .build());

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                        Текущее описание:""")
                    .build());

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                        %s""", editedEvent.getDescription()))
                    .build());

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Введите новое описание мероприятия:""")
                    .build());

            stateManager.setUserState(chatId, BotState.EDITING_EVENT_DESCRIPTION);
        }
    }

    public void checkEditedEventDescription(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String userMessage = update.getMessage().getText();
        int maxDescriptioonLength = 2000;

        Optional<Event> eventOptional = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId);

        if (userMessage.isBlank()) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Описание мероприятия не может быть пустым(""")
                    .build());
        } else if (userMessage.length() > maxDescriptioonLength) {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Описание мероприятия не может быть больше %s символов(""",
                            maxDescriptioonLength))
                    .build());
        } else {
            Event event = eventOptional.get();
            event.setDescription(userMessage);

            eventRepository.save(event);
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новое описание мероприятия сохранено!""")
                    .build());

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Текущая обложка:""")
                    .build());

            InputStream fileStream = null;
            try {
                fileStream = imageService.getFile("pictures", event.getImageUrl());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            InputFile inputFile = new InputFile(fileStream, event.getImageUrl());

            telegramSender.sendPhoto(chatId, SendPhoto.builder()
                    .chatId(chatId)
                    .photo(inputFile)
                    .build());

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                Пришлите новую обложку мероприятия:""")
                    .build());

            stateManager.setUserState(chatId, BotState.EDITING_EVENT_PICTURE);
        }
    }

    public void checkEditedEventPicture(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.getMessage().hasDocument()) {
            String fileId = updateUtil.getFileId(update);
            try {
                Event event = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(chatId).get();
                String imageUrl = event.getImageUrl();
                if (imageUrl != null && !imageUrl.isBlank()) {
                    String bucketName = "pictures";
                    imageService.deleteImage(bucketName, imageUrl);
                }

                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("Новое изображение успешно сохранено.")
                        .build());

                stateManager.doneEventEditing(chatId);
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
