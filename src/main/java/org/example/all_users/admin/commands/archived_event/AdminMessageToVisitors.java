package org.example.all_users.admin.commands.archived_event;

import lombok.RequiredArgsConstructor;
import org.example.all_users.admin.commands.AdminStart;
import org.example.data_classes.data_base.entity.Event;
import org.example.data_classes.enums.UserState;
import org.example.repository.EventRepository;
import org.example.repository.EventVisitRepository;
import org.example.util.state.StateManager;
import org.example.util.telegram.api.TelegramSender;
import org.example.util.telegram.helpers.UpdateUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AdminMessageToVisitors {
    private final Map<Long, Long> eventIdMap = new ConcurrentHashMap<>();

    private final UpdateUtil updateUtil;
    private final TelegramSender telegramSender;
    private final StateManager stateManager;
    private final AdminStart adminStart;
    private final EventRepository eventRepository;
    private final EventVisitRepository eventVisitRepository;

    public void processCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = updateUtil.getChatId(update);
        String[] callbackTextArray = callbackData.split("_");

        switch (callbackTextArray[1]) {
            case "to-event-visitors" -> handleMessageToSubscribersCommand(chatId, callbackData, update);
            case "accept-to-event-visitors" -> acceptForwardMessages(chatId, callbackData, messageId, update);
            case "cancel-to-event-visitors" -> cancelForwardMessages(chatId, messageId, update);
            default -> sendUnknownCallbackResponse(chatId);
        }

        telegramSender.answerCallbackQuerry(chatId, AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text("""
                        Команда обработана.""")
                .showAlert(false)
                .build());
    }

    private void handleMessageToSubscribersCommand(Long chatId, String callbackText, Update update) {
        Long eventId = Long.parseLong(callbackText.split("_")[2]);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isPresent()) {
            ReplyKeyboardRemove replyKeyboardRemove = ReplyKeyboardRemove.builder()
                    .removeKeyboard(true)
                    .build();

            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .replyMarkup(replyKeyboardRemove)
                    .text(String.format("""
                        Пришлите сюда сообщение, а мы отправим его всем пользователям, пришедшим на мероприятие: *%s*""", eventOptional.get().getEventName()))
                    .build());

            eventIdMap.put(chatId, eventId);
            stateManager.setUserState(chatId, UserState.ACCEPTING_FORWARD_MESSAGE_TO_EVENT_VISITORS);
        } else {
            telegramSender.sendText(chatId, SendMessage.builder()
                    .chatId(chatId)
                    .text("""
                                    Мероприятие не найдено.""")
                    .build());
            adminStart.handleStartState(update);
        }
    }

    private void sendUnknownCallbackResponse(Long chatId) {
        SendMessage unknownCallbackMessage = new SendMessage();
        unknownCallbackMessage.setChatId(chatId.toString());
        unknownCallbackMessage.setText("Неизвестная команда.");

        telegramSender.sendText(chatId, unknownCallbackMessage);
    }

    public void acceptingForwardMessageToEventVisitors(Update update) {
        Long chatId = updateUtil.getChatId(update);
        Integer forwardMessageId = update.getMessage().getMessageId();

        InlineKeyboardButton yesButton = InlineKeyboardButton.builder()
                .text("Да")
                .callbackData("message-to-visitors_accept-to-event-visitors_" + forwardMessageId)
                .build();

        InlineKeyboardButton noButton = InlineKeyboardButton.builder()
                .text("Нет")
                .callbackData("message-to-visitors_cancel-to-event-visitors")
                .build();

        InlineKeyboardMarkup inlineKeyboardMarkup = InlineKeyboardMarkup.builder()
                .clearKeyboard()
                .keyboardRow(List.of(yesButton, noButton))
                .build();

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Разослать это сообщение?""")
                .replyMarkup(inlineKeyboardMarkup)
                .build());
    }

    private void acceptForwardMessages(Long chatId, String callbackText, Integer botMessageId, Update update) {
        Long eventId = eventIdMap.get(chatId);
        List<Long> allVisitorsChatId = eventVisitRepository.getVisitorsChatIds(eventId);
        Integer messageId = Integer.parseInt(callbackText.split("_")[2]);

        if (!allVisitorsChatId.isEmpty()) {
            allVisitorsChatId.forEach(visitorChatId -> {
                telegramSender.forwardMessage(chatId, ForwardMessage.builder()
                        .fromChatId(chatId)
                        .chatId(visitorChatId)
                        .messageId(messageId)
                        .build());
            });
        }

        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Сообщения отправлены.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(botMessageId)
                .build());

        adminStart.handleStartState(update);
    }

    private void cancelForwardMessages(Long chatId, Integer botMessageId, Update update) {
        telegramSender.sendText(chatId, SendMessage.builder()
                .chatId(chatId)
                .text("""
                        Рассылка отменена.""")
                .build());

        telegramSender.deleteMessage(chatId, DeleteMessage.builder()
                .chatId(chatId)
                .messageId(botMessageId)
                .build());

        adminStart.handleStartState(update);
    }
}
