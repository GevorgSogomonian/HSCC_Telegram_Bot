//package org.example.util;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class PictureGetter {
//    public void sendPhotoToUser(String chatId, String fileId, String minioFilePath) {
//        // Шаг 1: Проверить доступность fileId
//        boolean isFileIdValid = checkFileIdValidity(fileId, chatId);
//
//        if (isFileIdValid) {
//            // Шаг 2: Если fileId валиден, отправляем его напрямую
//            sendPhotoWithFileId(chatId, fileId);
//        } else {
//            // Шаг 3: Если fileId недоступен, загружаем файл из MinIO
//            String newFileId = uploadPhotoFromMinio(chatId, minioFilePath);
//
//            if (newFileId != null) {
//                // Обновляем fileId в базе данных
//                updateFileIdInDatabase(newFileId);
//            }
//        }
//    }
//
//    // Проверяем доступность fileId
//    private boolean checkFileIdValidity(String fileId, String chatId) {
//        try {
//            SendPhoto sendPhotoRequest = new SendPhoto();
//            sendPhotoRequest.setChatId(chatId);
//            sendPhotoRequest.setPhoto(new InputFile(fileId));
//
//            // Пробуем отправить фото
//            execute(sendPhotoRequest);
//            return true; // Если отправка успешна, fileId валиден
//        } catch (TelegramApiException e) {
//            System.err.println("FileId недействителен: " + e.getMessage());
//            return false; // Ошибка говорит о том, что fileId недоступен
//        }
//    }
//
//    // Отправляем фото напрямую с серверов Telegram
//    private void sendPhotoWithFileId(String chatId, String fileId) {
//        try {
//            SendPhoto sendPhotoRequest = new SendPhoto();
//            sendPhotoRequest.setChatId(chatId);
//            sendPhotoRequest.setPhoto(new InputFile(fileId));
//            execute(sendPhotoRequest);
//        } catch (TelegramApiException e) {
//            e.printStackTrace();
//        }
//    }
//
//    // Загружаем фото из MinIO и отправляем в Telegram
//    private String uploadPhotoFromMinio(String chatId, String minioFilePath) {
//        try {
//            // Здесь вы загружаете файл из MinIO
//            byte[] fileBytes = downloadFileFromMinio(minioFilePath);
//
//            // Отправляем файл в Telegram
//            SendPhoto sendPhotoRequest = new SendPhoto();
//            sendPhotoRequest.setChatId(chatId);
//            sendPhotoRequest.setPhoto(new InputFile(new ByteArrayInputStream(fileBytes), "image.jpg"));
//            Message message = execute(sendPhotoRequest);
//
//            // Получаем новый fileId
//            if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
//                return message.getPhoto().get(0).getFileId();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    // Обновляем fileId в базе данных
//    private void updateFileIdInDatabase(String newFileId) {
//        // Реализация обновления fileId в вашей базе данных
//    }
//}
