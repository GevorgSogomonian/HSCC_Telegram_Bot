package org.example.all_users.base_user.commands;

import lombok.RequiredArgsConstructor;
import org.example.data_classes.data_base.comparison_tables.EventMissing;
import org.example.data_classes.data_base.entity.Event;
import org.example.data_classes.data_base.comparison_tables.EventSubscription;
import org.example.data_classes.data_base.entity.Usr;
import org.example.data_classes.enums.UserState;
import org.example.repository.EventMissingRepository;
import org.example.repository.MySQLInfo;
import org.example.repository.EventRepository;
import org.example.repository.EventSubscriptionRepository;
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
public class BaseSubscribeToEvent {

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final MySQLInfo mySQLInfo;
    private final StateManager stateManager;
    private final EventMissingRepository eventMissingRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "offer-subscribe-to-event" -> handleSubscribeToEvent(chatId, callbackData, messageId);
            case "accept-subscribing" -> acceptSubscribe(chatId, callbackData, messageId);
            case "cancel-subscribing" -> cancelSubscribe(chatId, messageId);
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

            if (eventSubscriptionRepository.existsByChatIdAndEventId(chatId, eventId)) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                            Вы уже подписаны на это мероприятие.""")
                        .build());

                stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
                return;
            }

            String eventName = eventOptional.get().getEventName();
            InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                    .text("Да")
                    .callbackData(String.format("subscribe_accept-subscribing_%s_old-message-id_%s", eventId, messageId))
                    .build();

            InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                    .text("Нет")
                    .callbackData(String.format("subscribe_cancel-subscribing_%s_old-message-id_%s", eventId, messageId))
                    .build();

            InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                    .clearKeyboard()
                    .keyboardRow(List.of(yesButton, noButton))
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                            Вы уверены, что хотите подписаться на мероприятие: *%s* ?""", eventName))
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

    public void acceptSubscribe(Long chatId, String callbackText, Integer messageId) {
        String[] callbackTextArray = callbackText.split("_");
        Long eventId = Long.parseLong(callbackTextArray[2]);
        Integer oldMessageId = Integer.parseInt(callbackTextArray[4]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        Optional<Usr> userOptional = userRepository.findByChatId(chatId);

        if (eventOptional.isPresent() && userOptional.isPresent()) {

            if (eventSubscriptionRepository.existsByChatIdAndEventId(chatId, eventId)) {
                telegramSender.sendText(chatId, SendMessage.builder()
                        .chatId(chatId)
                        .text("""
                            Вы уже подписаны на это мероприятие.""")
                        .build());

                stateManager.setUserState(chatId, UserState.COMMAND_CHOOSING);
                return;
            }

            Event event = eventOptional.get();
            Usr user = userOptional.get();
            String eventName = event.getEventName();

            EventSubscription eventSubscription = new EventSubscription();
            eventSubscription.setChatId(chatId);
            eventSubscription.setEventId(eventId);
            eventSubscriptionRepository.save(eventSubscription);

            EventMissing eventMissing = new EventMissing();
            eventMissing.setChatId(chatId);
            eventMissing.setEventId(eventId);
            eventMissingRepository.save(eventMissing);

            userRepository.save(user);

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text(String.format("""
                    Вы подписались на мероприятие *%s* !
                    
                    За сутки до начала, мы пришлём вам напоминание.""", eventName))
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

    public void cancelSubscribe(Long chatId, Integer messageId) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Подписка отменена.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }
}
