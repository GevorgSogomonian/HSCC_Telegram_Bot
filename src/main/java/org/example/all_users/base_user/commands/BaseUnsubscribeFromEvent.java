package org.example.all_users.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.entity.Event;
import org.example.data_classes.data_base.entity.Usr;
import org.example.data_classes.enums.UserState;
import org.example.repository.EventRepository;
import org.example.repository.EventSubscriptionRepository;
import org.example.repository.MySQLInfo;
import org.example.repository.UserRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaseUnsubscribeFromEvent {

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final MySQLInfo mySQLInfo;
    private final StateManager stateManager;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "offer-unsubscribe-from-event" -> handleSubscribeToEvent(chatId, callbackData, messageId);
            case "accept-unsubscribing" -> acceptUnsubscribe(chatId, callbackData, messageId);
            case "cancel-unsubscribing" -> cancelUnsubscribe(chatId, messageId);
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

    public void handleSubscribeToEvent(Long chatId, String callbackText, Integer messageId) {
        String[] callbackTextArray = callbackText.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            Event event = eventOptional.get();
            if (mySQLInfo.getCurrentTimeStamp().isAfter(event.getStartTime())) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                            Регистрация на мероприятие закрыта.""")
                        .build());

                stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
                return;
            }
            String eventName = eventOptional.get().getEventName();
            InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                    .text("Да")
                    .callbackData(String.format("unsubscribe_accept-unsubscribing_%s_old-message-id_%s", eventId, messageId))
                    .build();

            InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                    .text("Нет")
                    .callbackData(String.format("unsubscribe_cancel-unsubscribing_%s_old-message-id_%s", eventId, messageId))
                    .build();

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(yesButton, noButton))
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                            Вы уверены, что хотите отписаться от мероприятие: *%s* ?""", eventName))
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

    public void acceptUnsubscribe(Long chatId, String callbackText, Integer messageId) {
        String[] callbackTextArray = callbackText.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        Integer oldMessageId = Integer.parseInt(callbackTextArray[4]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        Optional<Usr> userOptional = userRepository.findByChatId(chatId);

        if (eventOptional.isPresent() && userOptional.isPresent()) {

            List<Long> subscribedEventIds = eventSubscriptionRepository.getSubscribedEventIds(chatId);
            if (subscribedEventIds == null || subscribedEventIds.isEmpty()) {
                telegramSender.sendText(chatId, SendMessage.builder()
                                .chatId(chatId)
                                .text("""
                                        У вас нет активных подписок на мероприятия.""")
                        .build());
            } else if (!subscribedEventIds.contains(eventId)) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                                    Вы не были подписаны на это мероприятие.""")
                        .build());
            } else {
                eventSubscriptionRepository.removeEventSubscriptionByEventIdAndChatId(eventId, chatId);
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format("""
                                            Вы отписались от мероприятия: *%s* .""", eventOptional.get().getEventName()))
                        .build());
            }
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

    public void cancelUnsubscribe(Long chatId, Integer messageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Отписка отменена.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }
}
