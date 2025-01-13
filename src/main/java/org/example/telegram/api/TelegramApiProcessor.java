package org.example.telegram.api;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ChatBotRequest;
import org.example.dto.ChatBotResponse;
import org.example.entity.Event;
import org.example.repository.EventRepository;
import org.example.repository.UserRepository;
import org.example.util.image.ImageService;
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

import java.io.InputStream;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramApiProcessor {

    private final TelegramApiQueue telegramApiQueue;
    private final TelegramBotService telegramBotService;
    private final ImageService imageService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

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
                        // Здесь можно добавить логику для обновления мероприятия

                        Long userId = userRepository.findByChatId(chatId).get().getChatId();
                        Event event = eventRepository.findFirstByCreatorChatIdOrderByUpdatedAtDesc(userId).get();
                        event.setImageUrl(imageUrl);
                        eventRepository.save(event);
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
                    PartialBotApiMethod<?> method = chatBotResponse.getMethod();

                    if (method instanceof SendPhoto sendPhoto) {
                        sendPhoto.setParseMode("Markdown");
                        telegramBotService.execute(sendPhoto);
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
}