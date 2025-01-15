package org.example.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.dto.ChatBotRequest;
import org.example.entity.UserState;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.util.StringValidator;
import org.example.util.TemporaryDataService;
import org.example.image.ImageService;
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

    private final TemporaryDataService<Event> temporaryEditedEventService;
    private final StringValidator stringValidator;

    public void handleEditEvent(Long chatId, Long eventId) {
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event editedEvent = eventOptional.get();
            temporaryEditedEventService.putTemporaryData(chatId, editedEvent);

            requestNewEventName(chatId, editedEvent);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                            .chatId(chatId)
                            .text("""
                                    Мероприятие не найдено!""")
                    .build());
        }
    }

    private void requestNewEventName(Long chatId, Event event) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Текущее название:""")
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text(String.format("""
                        *%s*""", event.getEventName()))
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                            Введите новое название для мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_NAME);
    }

    public void checkEditedEventName(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String eventName = update.getMessage().getText();
        Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
        Long eventId = editedEvent.getId();
        Optional<Event> eventOptional = eventRepository.findByEventName(editedEvent.getEventName());
        String validatedEventName = stringValidator.validateEventName(chatId, eventName);

        if (!validatedEventName.isEmpty()) {
            if (eventOptional.isPresent() && !eventOptional.get().getId().equals(eventId)) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                Мероприятие с названием: *%s* уже существует.
                                
                                В разделе 'Все мероприятия' вы можете удалять и редактировать свои мероприятия""", validatedEventName))
                        .build());
            } else {
                editedEvent.setEventName(validatedEventName);
                temporaryEditedEventService.putTemporaryData(chatId, editedEvent);

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Отлично! Название сохранено!""")
                        .build());

                requestNewEventDescription(chatId, editedEvent);
            }
        }
    }

    private void requestNewEventDescription(Long chatId, Event editedEvent) {
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

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_DESCRIPTION);
    }

    public void checkEditedEventDescription(Update update) {
        Long chatId = updateUtil.getChatId(update);
        String eventDescription = update.getMessage().getText();
        String validatedEventDescription = stringValidator.validateEventDescription(chatId, eventDescription);

        if (!validatedEventDescription.isEmpty()) {
            Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
            editedEvent.setDescription(eventDescription);
            temporaryEditedEventService.putTemporaryData(chatId, editedEvent);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Отлично! Новое описание мероприятия сохранено!""")
                    .build());

            requestNewEventPicture(chatId, editedEvent);
        }
    }

    private void requestNewEventPicture(Long chatId, Event editedEvent) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                                Текущая обложка:""")
                .build());

        telegramSender.sendPhoto(chatId, editedEvent.getId(), SendPhoto.builder()
                .chatId(chatId)
                .build());

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Пришлите новую обложку мероприятия:""")
                .build());

        stateManager.setUserState(chatId, UserState.EDITING_EVENT_PICTURE);
    }

    public void checkEditedEventPicture(Update update) {
        Long chatId = updateUtil.getChatId(update);

        if (update.getMessage().hasDocument()) {
            String fileId = updateUtil.getFileId(update);
            try {
                Event editedEvent = temporaryEditedEventService.getTemporaryData(chatId);
                String imageUrl = editedEvent.getImageUrl();
                if (imageUrl != null && !imageUrl.isBlank()) {
                    String bucketName = "pictures";
                    imageService.deleteImage(bucketName, imageUrl);
                    System.out.println("""
                            Сработал удаление)""");
                }

                telegramApiQueue.addRequest(new ChatBotRequest(chatId, new GetFile(fileId)));

                Thread pictureSaveThread = new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    eventRepository.save(temporaryEditedEventService.getTemporaryData(chatId));
                    System.out.println("""
                            Сработал сохранение)""");
                });
                pictureSaveThread.start();

                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                Новое изображение сохранено.""")
                        .build());

                stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
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
