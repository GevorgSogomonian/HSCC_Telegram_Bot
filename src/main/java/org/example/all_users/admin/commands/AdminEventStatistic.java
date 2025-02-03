package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.repository.CsvExportRepository;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class AdminEventStatistic {

    private final CsvExportRepository csvExportRepository;
    private final TelegramSender telegramSender;
    private final UpdateUtil updateUtil;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "archived-event" -> sendArchivedEventStatistic(chatId, callbackData);
            case "actual-event" -> sendActualEventStatistic(chatId, callbackData);
            default -> sendUnknownCallbackResponse(chatId);
        }

        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    public void sendArchivedEventStatistic(Long chatId, String callbackData) {
        String[] callbackTextArray = callbackData.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        byte[] csvData = csvExportRepository.archivedEventCSVStatistic(eventId);
        InputStream inputStream = new ByteArrayInputStream(csvData);

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId.toString());
        sendDocument.setDocument(new InputFile(inputStream, "archived_data.csv"));

        telegramSender.sendDocument(chatId, sendDocument);
    }

    public void sendActualEventStatistic(Long chatId, String callbackData) {
        String[] callbackTextArray = callbackData.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        byte[] csvData = csvExportRepository.actualEventCSVStatistic(eventId);
        InputStream inputStream = new ByteArrayInputStream(csvData);

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId.toString());
        sendDocument.setDocument(new InputFile(inputStream, "actual_data.csv"));

        telegramSender.sendDocument(chatId, sendDocument);
    }
}
