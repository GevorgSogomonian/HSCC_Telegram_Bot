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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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

    @Value("${spring.minio.bucketName}")
    private String bucketName;

    @PostConstruct
    public void startProcessing() {
        new Thread(() -> {
            while (true) {
                try {
                    ChatBotRequest chatBotRequest = telegramApiQueue.takeRequest();
                    Long chatId = chatBotRequest.getChatId();
                    BotApiMethod<?> method = chatBotRequest.getMethod();

                    if (method instanceof GetFile getFile) {
                        getFileAndSave(chatId, getFile);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                try {
                    ChatBotResponse chatBotResponse = telegramApiQueue.takeResponse();
                    PartialBotApiMethod<?> method = chatBotResponse.getMethod();

                    if (method instanceof SendPhoto sendPhoto) {
                        sendPhoto.setParseMode("Markdown");
                        if (chatBotResponse.getEventId() != null) {
                            sendEvent(chatBotResponse, sendPhoto);
                        }
                    } else if (method instanceof SendMessage sendMessage) {
                        sendMessage.setParseMode("Markdown");
                        telegramBotService.execute(sendMessage);
                    } else if (method instanceof AnswerCallbackQuery answerCallbackQuery) {
                        telegramBotService.execute(answerCallbackQuery);
                    } else if (method instanceof DeleteMessage deletemessage) {
                        telegramBotService.execute(deletemessage);
                    } else if (method instanceof ForwardMessage forwardMessage) {
                        telegramBotService.execute(forwardMessage);
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

    private void getFileAndSave(Long chatId, GetFile getFile) throws Exception {
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

        String imageUrl = imageService.uploadImage(multipartFile);

        Event event = temporaryEventService.getTemporaryData(chatId);
        event.setImageUrl(imageUrl);
        temporaryEventService.putTemporaryData(chatId, event);
    }

    private void sendEvent(ChatBotResponse chatBotResponse, SendPhoto sendPhoto) {
        Long eventId = chatBotResponse.getEventId();
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            String telegramFileId = event.getTelegramFileId();
            checkFileIdValidity(telegramFileId, sendPhoto, event);
        }
    }

    private void checkFileIdValidity(String telegramFileId, SendPhoto sendPhoto, Event event) {

        if (telegramFileId != null) {
            try {
                sendFileFromTelegramCache(telegramFileId, sendPhoto);
            } catch (TelegramApiException e) {
                sendFileFromMinio(sendPhoto, event);
            }
        } else {
            sendFileFromMinio(sendPhoto, event);
        }
    }

    private void sendFileFromTelegramCache(String telegramFileId, SendPhoto sendPhoto) throws TelegramApiException {
        sendPhoto.setPhoto(new InputFile(telegramFileId));
        System.out.println("Trying to sent photo directly from telegram");
        telegramBotService.execute(sendPhoto);
    }

    private void sendFileFromMinio(SendPhoto sendPhoto, Event event) {
        InputStream inputStream;
        try {
            inputStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(event.getImageUrl())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        InputStreamResource resource = new InputStreamResource(inputStream);

        sendPhoto.setPhoto(new InputFile(resource.getInputStream(), event.getImageUrl()));
        Message message;
        try (resource) {
            message = telegramBotService.execute(sendPhoto);
        } catch (TelegramApiException | IOException ex) {
            throw new RuntimeException(ex);
        }
        if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
            event.setTelegramFileId(message.getPhoto().get(0).getFileId());
            eventRepository.save(event);
        }
    }
}