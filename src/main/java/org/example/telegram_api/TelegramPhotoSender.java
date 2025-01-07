//package org.example.telegram_api;
//
//import lombok.RequiredArgsConstructor;
//import org.example.service.TelegramBotService;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
//import org.telegram.telegrambots.meta.api.objects.InputFile;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
//
//import java.io.InputStream;
//
//@Component
//@RequiredArgsConstructor
//public class TelegramPhotoSender {
//
//    private final TelegramBotService telegramBotService;
//
//    public void sendPhoto(Long chatId, InputStream fileStream, String fileName) {
//        try {
//            InputFile inputFile = new InputFile(fileStream, fileName);
//            SendPhoto sendPhoto = new SendPhoto();
//            sendPhoto.setChatId(chatId.toString());
//            sendPhoto.setPhoto(inputFile);
//
//            telegramBotService.execute(sendPhoto);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }
//}