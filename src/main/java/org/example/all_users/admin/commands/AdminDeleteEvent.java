package org.example.all_users.admin.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.Event;
import org.example.repository.EventDestructorRepository;
import org.example.repository.EventMissingRepository;
import org.example.repository.EventNotificationRepository;
import org.example.repository.EventRepository;
import org.example.repository.EventSubscriptionRepository;
import org.example.repository.EventVisitRepository;
import org.example.util.image.ImageService;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminDeleteEvent {
    private final EventRepository eventRepository;
    private final ImageService imageService;
    private final TelegramSender telegramSender;
    private final UpdateUtil updateUtil;
    private final EventNotificationRepository eventNotificationRepository;
    private final EventDestructorRepository eventDestructorRepository;
    private final EventMissingRepository eventMissingRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventVisitRepository eventVisitRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "offer-deleting-event" -> handleDeleteEvent(chatId, callbackData, messageId);
            case "accept-deleting-event" -> acceptDeletingEvent(chatId, callbackData, messageId);
            case "cancel-deleting-event" -> cancelDeletingEvent(chatId, messageId);
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

    public void handleDeleteEvent(Long chatId, String callbackText, Integer messageId) {
        String[] callbackTextArray = callbackText.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            String eventName = eventOptional.get().getEventName();
            InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                    .text("Да")
                    .callbackData(String.format("delete_accept-deleting-event_%s_old-message-id_%s", eventId, messageId))
                    .build();

            InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                    .text("Нет")
                    .callbackData(String.format("delete_cancel-deleting-event_%s_old-message-id_%s", eventId, messageId))
                    .build();

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(yesButton, noButton))
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                            Вы уверены, что хотите удалить мероприятие: *%s* ?""", eventName))
                    .replyMarkup(inlineKeyboardMarkup)
                    .build());
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятие не найдено.""")
                    .build());

            telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
        }
    }

    public void acceptDeletingEvent(Long chatId, String callbackText, Integer messageId) {
        String[] callbackTextArray = callbackText.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        Integer oldMessageId = Integer.parseInt(callbackTextArray[4]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            String eventName = event.getEventName();

            String imageUrl = event.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                String bucketName = "pictures";
                imageService.deleteImage(bucketName, imageUrl);
            }

            eventRepository.delete(event);
            deleteNotification(eventId);
            deleteDestructor(eventId);
            eventMissingRepository.removeEventMissingsByEventId(eventId);
            eventSubscriptionRepository.removeEventSubscriptionByEventId(eventId);
            eventVisitRepository.removeEventVisitsByEventId(eventId);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                            Мероприятие *%s* удалено!""", eventName))
                    .build());
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Мероприятие не найдено!""")
                    .build());
        }

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(oldMessageId)
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    private void deleteNotification(Long eventId) {
        eventNotificationRepository.deleteNotification(eventId);
    }

    private void deleteDestructor(Long eventId) {
        eventDestructorRepository.deleteDestructor(eventId);
    }

    public void cancelDeletingEvent(Long chatId, Integer messageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Удаление отменено.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }
}
