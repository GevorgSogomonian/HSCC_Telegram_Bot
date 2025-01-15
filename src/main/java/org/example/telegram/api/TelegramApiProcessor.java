package org.example.telegram.api;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatBotRequest;
import org.example.dto.ChatBotResponse;
import org.example.entity.Event;
import org.example.entity.InputStreamResource;
import org.example.repository.EventRepository;
import org.example.util.TemporaryDataService;
import org.example.image.ImageService;
import org.example.telegram.TelegramBotService;
import org.example.util.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramApiProcessor {

    private final TemporaryDataService<Event> temporaryEventService;
    private final TelegramApiQueue telegramApiQueue;
    private final TelegramBotService telegramBotService;
    private final ImageService imageService;
    private final EventRepository eventRepository;
    private final MinioClient minioClient;

    @PostConstruct
    public void startProcessing() {
        new Thread(() -> {
            while (true) {
                try {
                    // Извлекаем запрос из очереди
                    ChatBotRequest chatBotRequest = telegramApiQueue.takeRequest();
                    Long chatId = chatBotRequest.getChatId();
                    BotApiMethod<?> method = chatBotRequest.getMethod();

                    if (method instanceof GetFile getFile) {
                        // Выполняем запрос
                        org.telegram.telegrambots.meta.api.objects.File telegramFile = telegramBotService.execute(getFile);
                        log.info("executing 'getFile'");
                        String filePath = telegramFile.getFilePath();
                        String mimeType = getMimeTypeByExtension(filePath);
                        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);

                        InputStream fileStream = telegramBotService.downloadFileAsStream(telegramFile.getFilePath());
                        MultipartFile multipartFile = new MockMultipartFile(
                                fileName,
                                filePath,
                                mimeType,
                                fileStream.readAllBytes()
                        );

                        // Загружаем изображение и обновляем мероприятие
                        String imageUrl = imageService.uploadImage(multipartFile);

                        Event event = temporaryEventService.getTemporaryData(chatId);
                        event.setImageUrl(imageUrl);
//                        event.setTelegramFileId(telegramFile.getFileId());
                        temporaryEventService.putTemporaryData(chatId, event);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    // Извлекаем запрос из очереди
                    ChatBotResponse chatBotResponse = telegramApiQueue.takeResponse();
//                    Long chatId = chatBotResponse.getChatId();
                    PartialBotApiMethod<?> method = chatBotResponse.getMethod();
//                    Message message;

                    if (method instanceof SendPhoto sendPhoto) {
                        sendPhoto.setParseMode("Markdown");
                        Long eventId = chatBotResponse.getEventId();
                        Optional<Event> eventOptional = eventRepository.findById(eventId);
                        if (eventOptional.isPresent()) {
                            Event event = eventOptional.get();
                            String telegramFileId = event.getTelegramFileId();
                            checkFileIdValidity(telegramFileId, sendPhoto, event);

//                            if (message != null && message.getPhoto() != null && !message.getPhoto().isEmpty()) {
//                                String newTelegramFileId = message.getPhoto().get(0).getFileId();
//                                event.setTelegramFileId(newTelegramFileId);
//                                eventRepository.save(event);
//                            }
                        }
//                        message = telegramBotService.execute(sendPhoto);
//                        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
//                            String newFileId = message.getPhoto().get(0).getFileId();
//                            Optional<Event> eventOptional = eventRepository.findByCreatorChatId(chatId);
//                            if (eventOptional.isPresent()) {
//                                Event event = eventOptional.get();
//                                String oldFileId = event.getTelegramFileId();
//                                if (!newFileId.equals(oldFileId)) {
//                                    event.setTelegramFileId(newFileId);
//                                    eventRepository.save(event);
//                                }
//                            }
//                        }
                    } else if (method instanceof SendMessage sendMessage) {
                        sendMessage.setParseMode("Markdown");
                        telegramBotService.execute(sendMessage);
                    } else if (method instanceof AnswerCallbackQuery answerCallbackQuery) {
                        telegramBotService.execute(answerCallbackQuery);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        return mimeTypes.getOrDefault(extension, "application/octet-stream");
    }

    private void checkFileIdValidity(String telegramFileId, SendPhoto sendPhoto, Event event) {
//        InputFile oldInputFile = sendPhoto.getPhoto();

        if (telegramFileId != null) {
            try {
                sendFileFromTelegramCache(telegramFileId, sendPhoto);
            } catch (TelegramApiException e) {
                sendFileFromMinio(sendPhoto, event);
//                System.out.println("FileId недействителен: " + e.getMessage());
//                System.out.println("Sending photo from minio");
//                sendPhoto.setPhoto(oldInputFile);
//                try {
//                    telegramBotService.execute(sendPhoto);
//                } catch (TelegramApiException ex) {
//                    throw new RuntimeException(ex);
//                }
//                return null; // Ошибка говорит о том, что fileId недоступен
            }
        } else {
//            sendPhoto.setPhoto(oldInputFile);
            sendFileFromMinio(sendPhoto, event);
//            Message message;
//            try {
//                System.out.println("Sending photo from minio");
//                message = telegramBotService.execute(sendPhoto);
//            } catch (TelegramApiException ex) {
//                throw new RuntimeException(ex);
//            }
//            if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
//                event.setTelegramFileId(message.getPhoto().get(0).getFileId());
//                eventRepository.save(event);
//            }
//            return null; // Ошибка говорит о том, что fileId недоступен
        }
    }

    private Message sendFileFromTelegramCache(String telegramFileId, SendPhoto sendPhoto) throws TelegramApiException {
        sendPhoto.setPhoto(new InputFile(telegramFileId));
        // Пробуем отправить фото

        System.out.println("Trying to sent photo directly from telegram");
        return telegramBotService.execute(sendPhoto); // Если отправка успешна, fileId валиден
    }

    private void sendFileFromMinio(SendPhoto sendPhoto, Event event) {
        InputStream inputStream;
        try {
            inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket("pictures")
                    .object(event.getImageUrl())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

// Оборачиваем в InputStreamResource
        InputStreamResource resource = new InputStreamResource(inputStream);

        sendPhoto.setPhoto(new InputFile(resource.getInputStream(), event.getImageUrl()));
        Message message;
        try (resource) { // Автоматически закроет InputStream
            message = telegramBotService.execute(sendPhoto);
        } catch (TelegramApiException | IOException ex) {
            throw new RuntimeException(ex);
        }
//        try {
//            System.out.println("Sending photo from minio");
//            message = telegramBotService.execute(sendPhoto);
//        } catch (TelegramApiException ex) {
//            throw new RuntimeException(ex);
//        }
        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            event.setTelegramFileId(message.getPhoto().get(0).getFileId());
            eventRepository.save(event);
        }
    }
}